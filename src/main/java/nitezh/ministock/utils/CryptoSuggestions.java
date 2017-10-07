package nitezh.ministock.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by shirland on 10/7/17.
 */

public class CryptoSuggestions {
    private static final String BASE_URL = "https://api.coinmarketcap.com/v1/ticker/";


    public static List<Map<String, String>> getSuggestions(String query) {List<Map<String, String>> suggestions = new ArrayList<>();
        String response;
        try {
            String url = BASE_URL + URLEncoder.encode(query, "UTF-8");
            Cache cache = new StorageCache(null);
            response = UrlDataTools.getCachedUrlData(url, cache, 86400);

        } catch (UnsupportedEncodingException e1) {
            response = null;
        }

        // Return if empty response
        if (response == null || response.equals("")) {
            return suggestions;
        }
        try {
            //coinmarket cap doesnt support fuzzy ticker lookup , but still returns array
            JSONArray jsonArray = new JSONArray(response);
            JSONObject jsonResponse = (JSONObject)jsonArray.get(0);
            Map<String, String> suggestion = new HashMap<>();
            suggestion.put("symbol", "$" + jsonResponse.getString("symbol"));
            suggestion.put("name", jsonResponse.getString("name"));
            suggestion.put("price", jsonResponse.getString("price_usd"));
            suggestions.add(suggestion);
        }


        catch (JSONException ignored) {

        }

        return suggestions;
    }
}
