/**
 * *****************************************************************************
 * Copyright (C) 2017 IECHOR ES, Spanish National Bioinformatics Institute (INB)
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

package es.iechor.bsc.elixibilitas.dao;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import es.bsc.inb.iechor.openebench.model.metrics.Metrics;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.EncoderContext;
import org.bson.json.JsonWriter;

/**
 * Utility class to get/put Metrics into MongoDB.
 * 
 * @author Dmitry Repchevsky
 */

public class MetricsDAO extends AbstractDAO<String> implements Serializable {
    
    public final static String COLLECTION = "metrics";
    
    public MetricsDAO(MongoDatabase database, String baseURI) {
        super(baseURI, database, COLLECTION);
    }

    @Override
    protected String createPK(String id) {
        return id;
    }
    
    @Override
    protected String getURI(String pk) {
        return baseURI + pk;
    }
    
    @Override
    protected String getType(String pk) {
        return "metrics";
    }

    @Override
    protected String getLabel(String pk) {
        final String[] nodes = pk.split("/");
        if (nodes.length > 0) {
            final String[] _id = nodes[0].split(":");
            return _id[_id.length > 1 ? 1 : 0];
        }
        
        return "";
    }

    @Override
    protected String getVersion(String pk) {
        final String[] nodes = pk.split("/");
        if (nodes.length > 0) {
            final String[] _id = nodes[0].split(":");
            if (_id.length > 2) {
                return _id[2];
            }
        }
        
        return "";
    }
    
    public List<Metrics> get() {
        List<Metrics> metrics = new ArrayList<>();

        try {
            final MongoCollection<Document> col = database.getCollection(collection);
            
            for (Document doc : col.find()) {
                metrics.add(deserialize(doc));
            }
        } catch(Exception ex) {
            Logger.getLogger(MetricsDAO.class.getName()).log(Level.SEVERE, null, ex);
        }

        return metrics;
    }
    
    public Metrics get(String id) {
        try {
            final MongoCollection<Document> col = database.getCollection(collection);

            final Document doc = col.find(Filters.eq("_id", id)).first();
            if (doc != null) {
                return deserialize(doc);
            }
        } catch(Exception ex) {
            Logger.getLogger(MetricsDAO.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }
    
    public String getJSON(String id) {
        final Document doc = getBSON(id);
        return doc != null ? doc.toJson() : null;
    }

    public Document getBSON(String id) {
        try {
            final MongoCollection<Document> col = database.getCollection(collection);

            Document doc = col.find(Filters.eq("_id", id)).first();
            if (doc != null) {
                doc.append("@id", baseURI + doc.remove("_id"));
                doc.append("@type", "metrics");
                doc.append("@license", LICENSE);
                
                return doc;
            }
        } catch(Exception ex) {
            Logger.getLogger(MetricsDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public String upsert(String user, String id, Metrics metrics) {
        try {            
            final String json = jsonb.toJson(metrics);
            return upsert(user, id, json);
        } catch (Exception ex) {
            Logger.getLogger(MetricsDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public String merge(String user, String id, Metrics metrics) {
        try {            
            final String json = jsonb.toJson(metrics);
            return merge(user, id, json);
        } catch (Exception ex) {
            Logger.getLogger(MetricsDAO.class.getName()).log(Level.SEVERE, id, ex);
        }
        return null;        
    }
    
    private Metrics deserialize(Document doc) {

        doc.append("@id", baseURI + doc.remove("_id"));
        doc.append("@type", "metrics");
                        
        try {
            return jsonb.fromJson(doc.toJson(), Metrics.class);
        } catch(Exception ex) {
            Logger.getLogger(MetricsDAO.class.getName()).log(Level.SEVERE, doc.getString("@id"), ex);
        }
        return null;
    }
    
    /**
     * Find metrics and write them into the reader.
     * 
     * @param writer - writer to write metrics into.
     * @param projections - properties to write or null for all.
     */
    public void write(Writer writer, List<String> projections) {
        try {
            final MongoCollection<Document> col = database.getCollection(collection);
            try (JsonWriter jwriter = new ReusableJsonWriter(writer)) {

                final DocumentCodec codec = new DocumentCodec() {
                    @Override
                    public void encode(BsonWriter writer,
                       Document document,
                       EncoderContext encoderContext) {
                            super.encode(jwriter, document, encoderContext);
                    }
                };
                jwriter.writeStartArray();
                
                FindIterable<Document> iterator = col.find().noCursorTimeout(true);

                if (projections != null && projections.size() > 0) {
                    BasicDBObject bson = new BasicDBObject();
                    bson.append("@timestamp", true);
                    for (String field : projections) {
                        bson.append(field, true);
                    }
                    iterator = iterator.projection(bson);
                }

                try (MongoCursor<Document> cursor = iterator.iterator()) {
                    while (cursor.hasNext()) {
                        final Document doc = cursor.next();

                        doc.append("@id", baseURI + doc.remove("_id"));
                        doc.append("@type", "metrics");
                        doc.append("@license", LICENSE);

                        doc.toJson(codec);
                    }
                }
                jwriter.writeEndArray();
            }
        } catch(Exception ex) {
            Logger.getLogger(MetricsDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
