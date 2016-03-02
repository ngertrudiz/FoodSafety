package uk.ac.abdn.foodsafety.provenance;

import java.io.InputStream;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Function;

import com.hp.hpl.jena.rdf.model.Model;

import uk.ac.abdn.foodsafety.common.FoodSafetyException;
import eu.larkc.csparql.cep.api.RdfStream;
import eu.larkc.csparql.core.engine.CsparqlEngineImpl;

/**
 * 
 * @author nhc
 *
 * A FoodSafetyEngine is a specific CsparqlEngine with methods
 * and queries for this project.
 */
public final class SingleTagEngine
    extends CsparqlEngineImpl 
    implements Function<ZonedDateTime, Consumer<Model>> {
    private final RdfStream rdfStream = new RdfStream("http://foodsafety/parsed");
    
    /**
     * Initializes this engine.
     */
    public SingleTagEngine() {
        this.initialize();
        this.registerStream(this.rdfStream);
        this.registerQueryFromResources("/window.sparql.txt");
    }
        
    /**
     * Registers a SPARQL query read from /src/main/resources
     * The query must be encoded in UTF-8.
     * A ConsoleFormatter will be added as observer to the query.
     * @param path The path to the query, relative to FoodSafety/src/main/resources,
     * example: "/myquery.sparql.txt"
     */
    private void registerQueryFromResources(final String path) {
        //Get InputStream for the file
        final InputStream resourceAsStream = SingleTagEngine.class.getResourceAsStream(path);
        Scanner scanner = null;
        try {
            //Read entire file as UTF-8 into String
            scanner = new Scanner(resourceAsStream, "UTF-8");
            final String text = scanner.useDelimiter("\\A").next();
            //Register query and add observer
            this.registerQuery(text, false);//.addObserver(formatter);
        } catch (final ParseException e) {
            throw FoodSafetyException.internalError(e);
        } finally { //Close Scanner
            if (scanner != null) {
                scanner.close();
            }
        }
        
    }
    
    @Override
    public Consumer<Model> apply(final ZonedDateTime t) {
        return (m -> this.add(t, m));
    }

    private void add(ZonedDateTime t, Model m) {
        // TODO
    }
}