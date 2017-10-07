package nitezh.ministock.dataaccess;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import nitezh.ministock.domain.StockQuote;
import nitezh.ministock.utils.Cache;
import nitezh.ministock.utils.UrlDataTools;

/**
 * Created by shirland on 10/7/17.
 */

public class CoinMarketcapQuoteRepository {

    private static final String BASE_URL = "https://api.coinmarketcap.com/v1/ticker/";

    public HashMap<String, StockQuote> getQuotes(Cache cache, List<String> symbols) {
        HashMap<String, StockQuote> quotes = new HashMap<>();
        JSONArray jsonArray;
        JSONObject quoteJson;

        for (String symbol : symbols) {
            try {
                jsonArray = this.retrieveQuotesAsJson(cache, Collections.singletonList(symbol));

                for (int i = 0; i < jsonArray.length(); i++) {
                    quoteJson = jsonArray.getJSONObject(i);
                    StockQuote quote = new StockQuote(
                            quoteJson.optString("symbol"),
                            quoteJson.optString("price"),
                            quoteJson.optString("change"),
                            quoteJson.optString("percent"),
                            quoteJson.optString("exchange"),
                            quoteJson.optString("volume"),
                            quoteJson.optString("name"),"0",Locale.US
                            );
                    quotes.put(quote.getSymbol(), quote);
                }
            } catch (JSONException e) {
            }
        }

        return quotes;
    }

    private String buildRequestUrl() {

        return BASE_URL;
    }

    JSONArray retrieveQuotesAsJson(Cache cache, List<String> symbols) throws JSONException {
        String url = this.buildRequestUrl();
        String data = UrlDataTools.getCachedUrlData(url, cache, 300);
        //String json = data.replace("//", "").replaceAll("\\\\", "");
        JSONArray currencies = new JSONArray(data);
        JSONArray ret = new JSONArray();
        //filter out interested symbols
        for (String symbol: symbols){
            for (int i = 0 ; i < currencies.length(); i ++ ){
                JSONObject currency = (JSONObject) currencies.get(i);
                if(currency.getString("symbol").equals(symbol.substring(1))){

                    //set expected Stock attrivutes
                    JSONObject match = new JSONObject();
                    match.put("symbol",symbol);
                    match.put("price", currency.getString("price_usd"));
                    match.put("percent", currency.getString("percent_change_24h"));
                    match.put("change", "0");
                    match.put("exchange", "BitTrex");
                    match.put("volume",currency.getString("24h_volume_usd"));
                    match.put("change", getCalculatedChange(match));

                    ret = ret.put(match);

                    break;
                }

            }

        }

        return ret;
    }

    private String getCalculatedChange(JSONObject match) {
        double change = 0.0;

        try {
            double price = match.getDouble("price");
            double percentChange = match.getDouble("percent");

            change = ( percentChange / 100 ) * price;

        } catch (JSONException e) {
            e.printStackTrace();
        }


        return String.valueOf(change);
    }
}
