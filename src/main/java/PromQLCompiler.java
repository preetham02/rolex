import ast.AggBy;
import ast.AggOverTime;
import ast.Aggregation;
import ast.BinaryOp;
import ast.GroupAggOverTime;
import ast.GroupInstantVector;
import ast.InstantVector;
import ast.MetricSelector;
import ast.NumberLiteral;
import ast.RangeVector;
import ast.Scalar;
import ast.SingularFunction;
import ast.GroupBy;

import java.util.ArrayList;
import java.util.List;

public class PromQLCompiler {

    public static AbstractSQLSubQuery compileAndOptimizeInstantVector(InstantVector vector, Long startTime, Long endTime, Long step, Long timeTs) {
        AbstractSQLSubQuery subquery = compileInstantVector(vector);
        pushDownTimeRangeFilter(subquery, startTime, endTime, timeTs);
        AbstractSQLSubQuery query=discardUnnecessaryTuples(subquery,startTime,endTime,step, timeTs);
        return query;
    }

    public static AbstractGroupSubQuery compileAndOptimizeGroupInstantVector(ast.GroupInstantVector vector, Long startTime, Long endTime, Long step, Long timeTs) {
        AbstractGroupSubQuery subquery = compileGroupInstantVector(vector);
        pushDownTimeRangeFilter(subquery, startTime, endTime,step, timeTs);
//        pushDownTimeRangeFilter(subquery, startTime, endTime);
//        AbstractSQLSubQuery query=discardUnnecessaryTuples(subquery,startTime,endTime,step);
        return subquery;
    }


    private static void pushDownTimeRangeFilter(AbstractGroupSubQuery query, Long startTime, Long endTime,Long step ,Long timeTs) {
        visitGroup(query, startTime, endTime, step, timeTs);
    }

    private static void visitGroup(AbstractGroupSubQuery query, Long startTime, Long endTime,Long step ,Long timeTs) {
        List<SelectSQLSubQuery.WhereCondition> whereConditions = new ArrayList<>();

        if(startTime != null && endTime != null && step != null) {
            whereConditions.add(new SelectSQLSubQuery.WhereCondition("timestamp", ">=", String.valueOf(startTime)));
            whereConditions.add(new SelectSQLSubQuery.WhereCondition("timestamp", "<=", String.valueOf(endTime)));
//            whereConditions.add( new SelectSQLSubQuery.WhereCondition( String.format(" ( timestamp - %s) %% %s ",startTime, step) , "=", "0"));
        }

        if(timeTs !=null){
            whereConditions.add(new SelectSQLSubQuery.WhereCondition("timestamp", "=", String.valueOf(timeTs)));
        }

        if(query instanceof AggBySQLSubQuery sub){
            sub.pushDownFilter(whereConditions);
        }else if(query instanceof GroupAggBySQLSubQuery sub){
            sub.pushDownFilter(whereConditions);
        } else if (query instanceof GroupAggSubQuery sub) {
            visitGroup(sub.groupSubQuery,startTime,endTime,step,timeTs);
        }else if(query instanceof GroupBySQLSubQuery sub){
            sub.pushDownFilter(whereConditions);
        }



    }

    private static  AbstractGroupSubQuery compileGroupInstantVector(GroupInstantVector vector) {

        if(vector instanceof AggBy aggBy){
            return new GroupAggBySQLSubQuery(aggBy.getFunctionName(), aggBy.getColumns(), aggBy.getMetricName(), aggBy.getFilters(), aggBy.getDuration());
        } else if (vector instanceof GroupBy groupBy) {
            return new GroupBySQLSubQuery(groupBy.getColumns(), groupBy.getMetricName(), groupBy.getFilters());
        } else if (vector instanceof GroupAggOverTime groupAggOverTime) {
            AbstractGroupSubQuery subQuery = compileGroupInstantVector(groupAggOverTime.getArg().getVector());
            List<String> groups = getGroupColumns(subQuery);
            return new GroupAggSubQuery(subQuery, groupAggOverTime.getFunctionName(), groups);
        }



        return null;


    }

    private static List<String> getGroupColumns(AbstractGroupSubQuery subQuery) {

        if(subQuery instanceof  GroupBySQLSubQuery groupBySQLSubQuery){
            return groupBySQLSubQuery.getColumns();
        } else if (subQuery instanceof GroupAggSubQuery groupAggSubQuery) {
            return groupAggSubQuery.getGroups();
        } else if (subQuery instanceof GroupAggBySQLSubQuery groupAggOverTime) {
            return groupAggOverTime.getColumns();
        }else if( subQuery instanceof AggBySQLSubQuery aggSQLSubQuery){
            return aggSQLSubQuery.getColumns();
        }
        return null;
    }







    private static void pushDownTimeRangeFilter(AbstractSQLSubQuery query, Long startTime, Long endTime, Long timeTs) {
        visit(query, startTime, endTime, timeTs);
    }

    private static AbstractSQLSubQuery discardUnnecessaryTuples(AbstractSQLSubQuery query, Long startTime, Long endTime, Long step, Long timeTs) {

//    if(startTime == null || endTime == null || step == null) {
//            return new ProjectSQLSubquery(
//                    List.of("`timestamp`","`value`"),
//                    query
//            );
//    }

    List<SelectSQLSubQuery.WhereCondition> whereConditions = new ArrayList<>();

    if(startTime != null && endTime != null && step != null) {
        whereConditions.add(new SelectSQLSubQuery.WhereCondition("timestamp", ">=", String.valueOf(startTime)));
        whereConditions.add(new SelectSQLSubQuery.WhereCondition("timestamp", "<=", String.valueOf(endTime)));
        whereConditions.add( new SelectSQLSubQuery.WhereCondition( String.format(" ( timestamp - %s) %% %s ",startTime, step) , "=", "0"));
    }

    if(timeTs !=null){
        whereConditions.add(new SelectSQLSubQuery.WhereCondition("timestamp", "=", String.valueOf(timeTs)));
    }

    if(whereConditions.isEmpty()){
        return new ProjectSQLSubquery(
                    List.of("`timestamp`","`value`"),
                    query
            );
    }



       SelectSQLSubQuery sqlSubQuery= new SelectSQLSubQuery(whereConditions,query);
    return new ProjectSQLSubquery(
            List.of("`timestamp`","`value`"),
            sqlSubQuery
    );

    }





    private static void visit(AbstractSQLSubQuery query, Long startTime, Long endTime, Long timeTs) {

        if(query instanceof AggSQLSubQuery) {
            AggSQLSubQuery aggQuery = (AggSQLSubQuery) query;
            visit(aggQuery.getSubQuery(), startTime, endTime,timeTs);
        } else if (query instanceof SelectSQLSubQuery) {
            SelectSQLSubQuery selectQuery = (SelectSQLSubQuery) query;
            visit(selectQuery.getInnerSubquery(), startTime, endTime, timeTs);
        } else if (query instanceof CollectionSQLSubQuery collectionQuery) {
            List<SelectSQLSubQuery.WhereCondition> timeConditions = new ArrayList<>();
            if (startTime != null) {
                SelectSQLSubQuery.WhereCondition startCondition = new SelectSQLSubQuery.WhereCondition("meta().id", ">=", "\""+(startTime)+"\"");
                timeConditions.add(startCondition);
            }
            if (endTime != null) {
                SelectSQLSubQuery.WhereCondition endCondition = new SelectSQLSubQuery.WhereCondition("meta().id", "<=", "\""+(endTime)+"\"");
                timeConditions.add(endCondition);
            }
            if(timeTs!=null){
                SelectSQLSubQuery.WhereCondition timeTsCondition = new SelectSQLSubQuery.WhereCondition("meta().id", "=", "\""+(timeTs)+"\"");
                timeConditions.add(timeTsCondition);
            }

            collectionQuery.setRangeFilters(timeConditions);
        }
        else if(query instanceof SingularFunctionSubQuery){
            SingularFunctionSubQuery singularFunctionSubQuery = (SingularFunctionSubQuery) query;
            visit(singularFunctionSubQuery.getInnerSubquery(), startTime, endTime, timeTs);
        } else if (query instanceof BinaryOpJoinSubQuery binaryOpJoinSubQuery) {
            visit(binaryOpJoinSubQuery.getLeftSubQuery(), startTime, endTime, timeTs);
            visit(binaryOpJoinSubQuery.getRightSubQuery(), startTime, endTime,timeTs);
        } else if (query instanceof BinaryOpScalarSubQuery binaryOpScalarSubQuery) {
            visit(binaryOpScalarSubQuery.getLeftSubQuery(), startTime, endTime, timeTs);
            visit(binaryOpScalarSubQuery.getRightSubQuery(), startTime, endTime, timeTs);

        } else if (query instanceof ScalarSubQuery scalarSubQuery) {
            visit(scalarSubQuery.getSubQuery(), startTime, endTime, timeTs);
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
