/*
 The MIT License

 Copyright (c) 2013 Nitesh Patel http://niteshpatel.github.io/ministocks

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */

package nitezh.ministock.dataaccess;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import nitezh.ministock.domain.StockQuote;
import nitezh.ministock.domain.WidgetRepository;
import nitezh.ministock.utils.Cache;
import nitezh.ministock.utils.UrlDataTools;


public class GoogleStockQuoteRepository {

    private static final String BASE_URL = "http://finance.google.com/finance?output=json&q=";
    private static WidgetRepository widgetRepository;
    

    private void enrichSymbols(final WidgetRepository widgetRepository){

    }

    public synchronized HashMap<String, StockQuote> getQuotes(Cache cache, List<String> symbols) {
        HashMap<String, StockQuote> quotes = new HashMap<>();
        JSONArray jsonArray;
        JSONObject quoteJson;

        for (String symbol : symbols) {
            try {
                jsonArray = this.retrieveQuotesAsJson(cache, symbol);

                for (int i = 0; i < jsonArray.length(); i++) {
                    quoteJson = jsonArray.getJSONObject(i);
                    StockQuote quote = null;
                    if (!(symbol.length() == 5 & symbol.endsWith("X"))) {
                        quote = new StockQuote(
                                quoteJson.optString("t"),
                                quoteJson.optString("l_cur", quoteJson.optString("l")).replace(",", ""),
                                quoteJson.optString("c"),
                                quoteJson.optString("cp"),
                                quoteJson.optString("e").replace("INDEX", ""),
                                quoteJson.optString("vo"),
                                quoteJson.optString("name"),
                                Locale.US);

                    } else { //mutual fund
                        quote = new StockQuote(
                                quoteJson.optString("t"),
                                quoteJson.optString("nav_prior"),
                                quoteJson.optString("nav_c"),
                                quoteJson.optString("nav_cp"),
                                quoteJson.optString("e"),
                                quoteJson.optString("vo"),
                                quoteJson.optString("name"),
                                Locale.US);
                    }
                    quotes.put(quote.getSymbol(), quote);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return quotes;
    }

    private String buildRequestUrl(String symbol) {

        return String.format("%s%s", BASE_URL, symbol);
    }

    public GoogleStockQuoteRepository(WidgetRepository w){
        this.widgetRepository = w;
    }

    JSONArray retrieveQuotesAsJson(Cache cache, String symbol) throws JSONException {

        String url = this.buildRequestUrl(symbol);
        String data = UrlDataTools.getCachedUrlData(url, cache, 300);
        String json = data.replace("//", "").replaceAll("\\\\", "");

        symbol = (symbol.contains(":")) ? symbol.split(":")[1] : symbol;
        //JSONObject raw = new JSONObject(json);

        //JSONArray vagueList = raw.getJSONArray("searchresults");

        //mutual fund response ...
        // Provided Android JSON libs don't support the response without these fixes

        if (symbol.length() == 5 && symbol.endsWith("X")){
            json=json.replace("\"Convertibles\",","\"Conv\"");
            json=json.replace("\"Corporate Bond\",","\"Corps\"");

            int problemIdx = json.indexOf("topholdings") - 11;
            String tmpString = json.substring(problemIdx);
            tmpString = tmpString.replaceFirst(",","");
            tmpString = json.substring(0,problemIdx) + tmpString;
            json = tmpString;

        }
        JSONArray ret;
        try {
            ret = new JSONArray(json);
            return ret;
        }catch (Exception e){
            JSONObject obj = new JSONObject(json);
            JSONArray arr = (JSONArray) new JSONObject(json).get("searchresults");

            //fuzzy quote response retry with exchange
            String exchange = json.substring(json.indexOf("\"ticker\" : " +"\"" + symbol )).split(":")[2].split("\n")[0].replace("\"","").replace(",","").trim();
            return retrieveQuotesAsJson(cache,exchange + ":" + symbol);

        }
    }
}
