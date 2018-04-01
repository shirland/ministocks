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

import org.jsoup.Jsoup;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import nitezh.ministock.domain.StockQuote;
import nitezh.ministock.domain.WidgetRepository;
import nitezh.ministock.utils.Cache;
import nitezh.ministock.utils.UrlDataTools;


public class CNBCQuoteRepository {

    private static final String BASE_URL = "https://www.cnbc.com/quotes/?symbol=";


    public synchronized HashMap<String, StockQuote> getQuotes(Cache cache, List<String> symbols) {
        HashMap<String, StockQuote> quotes = new HashMap<>();
        JSONObject quoteJson;

        for (String symbol : symbols) {

            quoteJson = this.retrieveQuoteAsJson(cache, symbol);

            StockQuote quote = null;

            quote = new StockQuote(
                    quoteJson.optString("symbol"),
                    quoteJson.optString("last"),
                    quoteJson.optString("change"),
                    quoteJson.optString("change_pct"),
                    quoteJson.optString("exchange"),
                    quoteJson.optString("fullVolume"),
                    quoteJson.optString("name"),
                    Locale.US);

            quotes.put(quote.getSymbol(), quote);

        }

        return quotes;
    }

    private String buildRequestUrl(String symbol) {

        return String.format("%s%s", BASE_URL, symbol);
    }

    JSONObject retrieveQuoteAsJson(Cache cache, String symbol) {

        String url = this.buildRequestUrl(symbol);
        String data = UrlDataTools.getCachedUrlData(url, cache, 300);
        JSONObject quoteDataJson = getSymbolInfoJSON(data);

        return quoteDataJson;

    }

    private JSONObject getSymbolInfoJSON(String data) {

        String rawResponse =  Jsoup.parse(data).select("script").get(1).data();

        try {
            return new JSONObject(rawResponse.substring(rawResponse.indexOf('{')));
        } catch (JSONException e1) {
            e1.printStackTrace();
        };
        return null;

    }
}
