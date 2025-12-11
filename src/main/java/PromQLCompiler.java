import ast.AggOverTime;
import ast.Aggregation;
import ast.BinaryOp;
import ast.InstantVector;
import ast.MetricSelector;
import ast.NumberLiteral;
import ast.RangeVector;
import ast.Scalar;
import ast.SingularFunction;

import java.util.ArrayList;
import java.util.List;

public class PromQLCompiler {

    public static AbstractSQLSubQuery compileAndOptimizeInstantVector(InstantVector vector, Long startTime, Long endTime, Long step) {
        AbstractSQLSubQuery subquery = compileInstantVector(vector);
        pushDownTimeRangeFilter(subquery, startTime, endTime);
        AbstractSQLSubQuery query=discardUnnecessaryTuples(subquery,startTime,endTime,step);
        return query;
    }


    private static void pushDownTimeRangeFilter(AbstractSQLSubQuery query, Long startTime, Long endTime) {
        visit(query, startTime, endTime);
    }

    private static AbstractSQLSubQuery discardUnnecessaryTuples(AbstractSQLSubQuery query, Long startTime, Long endTime, Long step) {

    if(startTime == null || endTime == null || step == null) {
            return query;
    }

       return new SelectSQLSubQuery(
                List.of(
                        new SelectSQLSubQuery.WhereCondition("timestamp", ">=", String.valueOf(startTime)),
                        new SelectSQLSubQuery.WhereCondition("timestamp", "<=", String.valueOf(endTime)),
                        new SelectSQLSubQuery.WhereCondition( String.format(" ( timestamp - %s) %% %s ",startTime, step) , "=", "0")
                ),query
        );
    }





    private static void visit(AbstractSQLSubQuery query, Long startTime, Long endTime) {

        if(query instanceof AggSQLSubQuery) {
            AggSQLSubQuery aggQuery = (AggSQLSubQuery) query;
            visit(aggQuery.getSubQuery(), startTime, endTime);
        } else if (query instanceof SelectSQLSubQuery) {
            SelectSQLSubQuery selectQuery = (SelectSQLSubQuery) query;
            visit(selectQuery.getInnerSubquery(), startTime, endTime);
        } else if (query instanceof CollectionSQLSubQuery collectionQuery) {
            List<SelectSQLSubQuery.WhereCondition> timeConditions = new ArrayList<>();
            if (startTime != null) {
                SelectSQLSubQuery.WhereCondition startCondition = new SelectSQLSubQuery.WhereCondition("timestamp", ">=", String.valueOf(startTime));
                timeConditions.add(startCondition);
            }
            if (endTime != null) {
                SelectSQLSubQuery.WhereCondition endCondition = new SelectSQLSubQuery.WhereCondition("timestamp", "<=", String.valueOf(endTime));
                timeConditions.add(endCondition);
            }
            collectionQuery.setRangeFilters(timeConditions);
        }
        else if(query instanceof SingularFunctionSubQuery){
            SingularFunctionSubQuery singularFunctionSubQuery = (SingularFunctionSubQuery) query;
            visit(singularFunctionSubQuery.getInnerSubquery(), startTime, endTime);
        } else if (query instanceof BinaryOpJoinSubQuery binaryOpJoinSubQuery) {
            visit(binaryOpJoinSubQuery.getLeftSubQuery(), startTime, endTime);
            visit(binaryOpJoinSubQuery.getRightSubQuery(), startTime, endTime);
        } else if (query instanceof BinaryOpScalarSubQuery binaryOpScalarSubQuery) {
            visit(binaryOpScalarSubQuery.getLeftSubQuery(), startTime, endTime);
            visit(binaryOpScalarSubQuery.getRightSubQuery(), startTime, endTime);

        } else if (query instanceof ScalarSubQuery scalarSubQuery) {
            visit(scalarSubQuery.getSubQuery(), startTime, endTime);
        }


    }



    private static AbstractSQLSubQuery compileInstantVector(InstantVector vector) {
        if(vector instanceof MetricSelector){
            return compileMetricSelector((MetricSelector) vector);
        } else if (vector instanceof SingularFunction) {
            return compileSingularFunction((SingularFunction) vector);
        } else if(vector instanceof ast.BinaryOp){
            return compileBinaryOp((BinaryOp)vector);
        } else if(vector instanceof AggOverTime){
            return compileAggOverTime((AggOverTime)vector);
        }

        return null; 
    }


    private  static AbstractSQLSubQuery compileMetricSelector(MetricSelector metricSelector) {
        List<SelectSQLSubQuery.WhereCondition> whereConditions = new ArrayList<>();
        for (var entry : metricSelector.getLabels().entrySet()) {
            SelectSQLSubQuery.WhereCondition condition = new SelectSQLSubQuery.WhereCondition(entry.getKey(), "=", "\"" + entry.getValue() + "\"");
            whereConditions.add(condition);
        }
        AbstractSQLSubQuery query = new CollectionSQLSubQuery(metricSelector.getMetricName());
        return new SelectSQLSubQuery(whereConditions, query);
    }


    private  static AbstractSQLSubQuery compileSingularFunction(SingularFunction singularFunction) {
        SingularFunctionSubQuery subQuery = new SingularFunctionSubQuery(singularFunction.getFunctionName(), compileInstantVector(singularFunction.getArg()));
        return subQuery;
    }

    private  static AbstractSQLSubQuery compileBinaryOp(BinaryOp binaryOp) {
        if(binaryOp.getRight() instanceof Scalar right){
            AbstractSQLSubQuery rightSubQuery = compileScalarNode(right);
            AbstractSQLSubQuery leftSubQuery = compileInstantVector(binaryOp.getLeft());
            return new BinaryOpScalarSubQuery(leftSubQuery, binaryOp.getOperator(), rightSubQuery);
        }else  if(binaryOp.getRight() instanceof InstantVector){
            return new BinaryOpJoinSubQuery(compileInstantVector(binaryOp.getLeft()), binaryOp.getOperator(), compileInstantVector((InstantVector) binaryOp.getRight()));
        }
        return null;
    }

    private  static AbstractSQLSubQuery compileAggOverTime(AggOverTime aggOverTime) {
        return new AggSQLSubQuery(compileInstantVector(aggOverTime.getArg().getVector()), AverageSQLSubquery.parseDuration(aggOverTime.getArg().getDuration()), aggOverTime.getFunctionName());
    }

    private static AbstractSQLSubQuery compileScalarNode(Scalar scalarNode) {
        if(scalarNode instanceof NumberLiteral numberLiteral){
            return new LiteralSubquery(String.valueOf(numberLiteral.getValue()));
        }else if (scalarNode instanceof Aggregation){
            return compileAggregation((Aggregation) scalarNode);
        }
        return null;
    }

    private static AbstractSQLSubQuery compileAggregation(Aggregation aggregation) {
        AbstractSQLSubQuery subQuery = compileInstantVector(aggregation.getArg());
        return new ScalarSubQuery(aggregation.getFunctionName(), subQuery);
    }



}
