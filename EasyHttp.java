import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
 
/**
 * Wrapper for HttpURLConnection optimized for GET, POST, and 
 * POST file operations returning JSON objects as responses.
 *
 * Parts adapted from www.codejava.net
 *
 */
public class EasyHttp{

    private HttpURLConnection connection;
    private JSONObject response;
    private String endpoint;

    public HttpUtility(String endpoint){
        this.endpoint = endpoint;
    }// end constructor
 
    public void setEndpoint(String endpoint){
        this.endpoint = endpoint;
    }

    public JSONObject getResponse(){
        return this.response;
    }

    // send GET request
    public HttpURLConnection sendGetRequest(Map<String, String> params) throws IOException {
        URL url = new URL(endpoint + serialize(params));
        connection = (HttpURLConnection) url.openConnection();
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(false);

        // fire connection and store response
        this.fire();
 
        return connection;
    }
 
    // send normal POST request
    public HttpURLConnection sendPostRequest(Map<String, String> params) 
            throws IOException {
        URL url = new URL(endpoint);
        connection = (HttpURLConnection) url.openConnection();
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);

        // send POST data
        if (params != null && params.size() > 0) {
            OutputStreamWriter writer = new OutputStreamWriter( connection.getOutputStream() );
            writer.write( serialize(params) );
            writer.flush();
        }
 
        // fire connection and store response
        this.fire();

        return connection;
    }
 
    // send file and some metadata via POST request. Boolean flag binary file
    // see https://www.w3.org/TR/html401/interact/forms.html#h-17.13.4.2
    public HttpURLConnection sendFile(File file, boolean binary, Map<String, String> meta) 
            throws IOException {

        URL url = new URL(endpoint);
        connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        String b = Long.toHexString(System.currentTimeMillis());
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + b);
        String CRLF = "\r\n";

        try (
            OutputStream output = connection.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"), true);
        ) {
            // send plaintext params
            for (Map.Entry<String, String> entry : meta.entrySet()){
                String key = URLEncoder.encode(entry.getKey(), "UTF-8");
                String val = URLEncoder.encode(entry.getValue(), "UTF-8");

                writer.append("--" + b).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"" + key + "\"").append(CRLF);
                writer.append(CRLF).append( val ).append(CRLF).flush();     // end of block
            }

            if(binary){         // Send binary file.
                writer.append("--" + b).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"").append(CRLF);
                writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(file.getName())).append(CRLF);
                writer.append("Content-Transfer-Encoding: binary").append(CRLF);
                writer.append(CRLF).flush();
                Files.copy(binaryFile.toPath(), output);
                output.flush();
                writer.append(CRLF).flush(); // end of block

            } else {            // send text file
                writer.append("--" + b).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"textFile\"; filename=\"" + textFile.getName() + "\"").append(CRLF);
                writer.append("Content-Type: text/plain; charset=UTF-8").append(CRLF); 
                writer.append(CRLF).flush();
                Files.copy(textFile.toPath(), output);
                output.flush();
                writer.append(CRLF).flush(); // end of block
            }

            // End of multipart/form-data
            writer.append("--" + b + "--").append(CRLF).flush();
        }

        // fire connection and store response
        this.fire();

        return connection;
    }

    // fires request & stores response
    public void fire() throws IOException {
        InputStream inputStream = null;
        if (connection != null) {
            inputStream = connection.getInputStream();
        } else {
            throw new IOException("Connection is not established.");
        }
 
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                inputStream));
        List<String> data = new ArrayList<String>();
 
        String line = "";
        while ((line = reader.readLine()) != null) {
            data.add(line);
        }
        reader.close();
        String json = Gson().toJson(data);

        response = (new JsonPrimitive(json)).getAsJsonObject();
    }
     
    // close connection
    public void disconnect() {
        if (connection != null) {
            connection.disconnect();
        }
    }

    private String serialize(Map<String, String> params){
        String serialized = "";
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()){
            if(!first){
                serialized += "&";
            } else {
                serialized = false;
            }
            serialized += URLEncoder.encode(entry.getKey(), "UTF-8") 
                + "=" + URLEncoder.encode(entry.getValue(), "UTF-8");
        }
        return serialized;
    }// end serialize method
    
}// end class