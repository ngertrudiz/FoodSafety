package uk.ac.abdn.foodsafety.provenance;

import java.time.ZonedDateTime;
import java.util.Observable;
import java.util.function.Consumer;
import java.util.function.Function;

import uk.ac.abdn.foodsafety.common.FoodSafetyException;
import uk.ac.abdn.foodsafety.common.Logging;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import eu.larkc.csparql.cep.api.RdfQuadruple;
import eu.larkc.csparql.cep.api.RdfStream;
import eu.larkc.csparql.common.RDFTable;
import eu.larkc.csparql.core.ResultFormatter;
import eu.larkc.csparql.core.engine.CsparqlEngineImpl;

/**
 * 
 * @author nhc
 *
 * A FoodSafetyEngine is a specific CsparqlEngine with methods
 * and queries for this project.
 */
public final class FoodSafetyEngine
    extends CsparqlEngineImpl 
    implements Function<ZonedDateTime, Consumer<Model>> {
    
    /** This engine's sole stream */
    private final RdfStream rdfStream = new RdfStream("http://foodsafety/ssn");
    
    /**
     * Initializes this engine.
     */
    public FoodSafetyEngine() {
        this.initialize();
        this.registerStream(this.rdfStream);
        new Configurator(this);
    }

    @Override
    public Consumer<Model> apply(final ZonedDateTime t) {
        return (m -> this.add(t, m));
    }

    private void add(final ZonedDateTime t, final Model m) {
        final long timestamp = t.toInstant().toEpochMilli();
        final StmtIterator it = m.listStatements();
        while (it.hasNext()) {
            final Statement triple = it.nextStatement();
            final RDFNode o = triple.getObject();
            if (o.isAnon()) {
                FoodSafetyException.internalError(String.format("Blank node in %s", m.toString()));
            };
            this.rdfStream.put(new RdfQuadruple(
                    triple.getSubject().getURI(),
                    triple.getPredicate().getURI(),
                    (o.isResource()) ? o.asResource().getURI() : o.asLiteral().getLexicalForm(),
                    timestamp));
        }
    }
    
    static class TmpFormatter extends ResultFormatter {
        /**
         * Called when C-Sparql emits a window.
         */
        @Override
        public void update(final Observable ignored, final Object rdfTableUntyped) {
            final RDFTable rdfTable = (RDFTable) rdfTableUntyped;
            Logging.info(String.format("Emitted %d triples", rdfTable.size()));
        }   
    }
}
