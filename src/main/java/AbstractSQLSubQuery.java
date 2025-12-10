public class AbstractSQLSubQuery {


    public static void main(String[] args) {
        System.out.println(PromServer.convertPromToSQL("avg_over_time(stock_market_price_usd[1h])", null,null,null));
    }
}
