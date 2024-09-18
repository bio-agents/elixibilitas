/**
 * *****************************************************************************
 * Copyright (C) 2018 IECHOR ES, Spanish National Bioinformatics Institute (INB)
 * and Barcelona Supercomputing Center (BSC)
 *
 * Modifications to the initial code base are copyright of their respective
 * authors, or their employers as appropriate.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *****************************************************************************
 */

package es.iechor.bsc.openebench.bioagents;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

/**
 * @author Dmitry Repchevsky
 */

public class BioagentsRepositoryIterator implements Iterator<JsonObject> {

    private int page;
    private Iterator<JsonObject> iterator;

    public BioagentsRepositoryIterator() {
        page = 1;
    }
    
    @Override
    public boolean hasNext() {
        if (iterator != null && iterator.hasNext()) {
            return true;
        }
        if (page > 0) {
            final List<JsonObject> agents = new ArrayList<>();
            page = next(agents, page);
            iterator = agents.iterator();
            return iterator.hasNext();
        }

        return false;
    }

    @Override
    public JsonObject next() {
        if (hasNext()) {
            return iterator.next();
        }
        
        return null;
    }
    
    /**
     * Get a next chunk of the agents from bio.agents registry
     * 
     * @param agents
     * @param page
     * @return 
     */
    private int next(List<JsonObject> agents, int page) {

        URL url;
        try {
            url = new URL("https://bio.agents/api/agent/?page=" + page);
        } catch(MalformedURLException ex) {
            return Integer.MIN_VALUE;
        }

        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json");
            try (InputStream in = con.getInputStream()) {
                JsonReader reader = Json.createReader(in);
                JsonObject jo = reader.readObject();
                JsonArray jagents = jo.getJsonArray("list");
                for (int i = 0, n = jagents.size(); i < n; i++) {
                    agents.add(jagents.getJsonObject(i));
                }
                String next = jo.getString("next", null);
                return next == null || !next.startsWith("?page=") ? Integer.MIN_VALUE : Integer.parseInt(next.substring(6));
            }
        } catch(Exception ex) {
            Logger.getLogger(BioagentsConverter.class.getName()).log(Level.WARNING, "error agents parsing, page " + page, ex);
            return Integer.MIN_VALUE;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }     
    }
}
