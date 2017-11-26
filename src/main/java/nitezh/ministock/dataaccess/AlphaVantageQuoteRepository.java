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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import nitezh.ministock.domain.StockQuote;
import nitezh.ministock.utils.Cache;
import nitezh.ministock.utils.UrlDataTools;


public class AlphaVantageQuoteRepository {

    private static final String ALPHAVANTAGE_API_KEY = "51DGREJ3JNRR60KV";
    private static final String BASE_URL = "https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&interval=15min&outputsize=compact&";
    private static final String FORMAT = "sd1t1l1c1p2xvn";
    private static final int COUNT_FIELDS = 9;
    private final FxChangeRepository fxChangeRepository;

    public AlphaVantageQuoteRepository(FxChangeRepository fxChangeRepository) {
        this.fxChangeRepository = fxChangeRepository;
    }

    public HashMap<String, StockQuote> getQuotes(Cache cache, List<String> symbols) {
        HashMap<String, StockQuote> quotes = new HashMap<>();
        HashMap<String, String> fxChanges = this.fxChangeRepository.getChanges(cache, symbols);
        JSONArray jsonArray;
        JSONObject quoteJson;

        try {

            //jsonArray = this.retrieveQuotesAsJson(cache, symbols);
            jsonArray = this.retrieveQuotesAsJson(cache, symbols);
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    quoteJson = jsonArray.getJSONObject(i);
                    StockQuote quote = new StockQuote(
                            quoteJson.optString("symbol"),
                            quoteJson.optString("price"),
                            quoteJson.optString("change"),
                            quoteJson.optString("percent"),
                            quoteJson.optString("exchange"),
                            quoteJson.optString("volume"),
                            quoteJson.optString("name"),
                            fxChanges.get(quoteJson.optString("symbol")),
                            Locale.US);
                    quotes.put(quote.getSymbol(), quote);
                }
            }
        } catch (JSONException e) {
            return null;
        }

        return quotes;
    }

    private String buildRequestUrl(String symbol) {
        return BASE_URL + "&apikey=" + ALPHAVANTAGE_API_KEY + "&symbol=" + symbol;
    }

    private JSONArray getQuotesCsv(Cache cache, List<String> symbols) {
        for (final String symbol : symbols) {
            String url = this.buildRequestUrl(symbol);

            String s = UrlDataTools.getCachedUrlData(url, cache, 300);
        }
        return new JSONArray();
    }

    private boolean isDataInvalid(String quotesCsv) {
        return (quotesCsv.equals("Missing Symbols List.") || quotesCsv.equals(""));
    }

    private String[] parseCsvLine(String line) {
        return line.replace("\"", "").split(",", COUNT_FIELDS);
    }

    private boolean isCsvLineInvalid(String[] values, List<String> symbols) {
        return (values.length < COUNT_FIELDS) || (!symbols.contains(values[0]));
    }

    public JSONArray retrieveQuotesAsJson(Cache cache, List<String> symbols) throws JSONException {

        JSONArray quotes = new JSONArray();

        for ( final String symbol : symbols){

            if ( symbol.startsWith("$")) //ignore crypto
                continue;


            String url = buildRequestUrl(symbol);
            String resp = UrlDataTools.getCachedUrlData(url,cache,300);
            JSONObject quote = processQuoteResponse(resp);
            quote.put("symbol",symbol);

            quotes = quotes.put(quote);

        }

        return quotes;
    }

    private JSONObject processQuoteResponse(String resp) {
        JSONObject ret = new JSONObject();

        try {
            JSONObject timeSeriesData = new JSONObject(resp).getJSONObject("Time Series (15min)");
            JSONObject latestQuote = timeSeriesData.getJSONObject(getNewestKeyByDate(timeSeriesData.keys()));

            ret.put("price",latestQuote.getDouble("4. close"));
            ret.put("volume",latestQuote.getDouble("5. volume"));
            ret.put("change",( latestQuote.getDouble("4. close") - latestQuote.getDouble("1. open") ));
            ret.put("percent",( ( latestQuote.getDouble("4. close") - latestQuote.getDouble("1. open") ) / latestQuote.getDouble("1. open")));
            ret.put("exchange","AlphaVantage");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ret;

    }

    private static String getNewestKey(Set<String> keys) {
        String newestKey = (String)keys.toArray()[0];
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        for (String s: keys){
            try {
                Date d = sdf.parse(s);
                Date newestDate = sdf.parse(newestKey);
                if ( d.after(newestDate))
                    newestKey = s;
            } catch (Exception e) {
            }
        }
        return newestKey;
    }

    private static String getNewestKeyByDate(Iterator<String> keys) {
        String newestKey = "1900-01-01 10:00:00";
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        while (keys.hasNext()){
            String s = keys.next();
            try {
                Date d = sdf.parse(s);
                Date newestDate = sdf.parse(newestKey);
                if ( d.after(newestDate))
                    newestKey = s;
            } catch (Exception e) {
            }
        }
        return newestKey;
    }

}
