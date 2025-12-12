public class AbstractSQLSubQuery {


    public static void main(String[] args) throws ParseException {
        try {
            ast.InstantVector iv = GeneratedPromQLParser.parseInstantVector("metric{label='value'} + 5");
            System.out.println("Instant Vector AST: " + iv);

            ast.RangeVector rv = GeneratedPromQLParser.parseRangeVector("metric{label='value'}[5m]");
            System.out.println("Range Vector AST: " + rv);


            ast.GroupInstantVector groupInstantVector = GroupPromQLParser.parseGroupInstant("group_by[region](ecommerce_sales_usd{category='books'})");
            System.out.println("Parsed Group AST: " + groupInstantVector);

            AbstractGroupSubQuery groupAst = PromQLCompiler.compileAndOptimizeGroupInstantVector(groupInstantVector,null,null,null);
            System.out.println("Group Instant AST: " + groupAst);

//            ast.GroupInstantVector recursiveGroup = GroupPromQLParser.parseGroupInstant("avg_over_time(sum_by[region)(metric)[1h])");
//            System.out.println("Recursive Group AST: " + recursiveGroup);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(PromServer.convertPromToSQL("avg_over_time(stock_market_price_usd[1h])", null,null,null));

        System.out.println(PromServer.convertPromToSQL("avg_over_time(stock_market_price_usd{status=\"500\"}[5m])", null,null,null));



    }
}
