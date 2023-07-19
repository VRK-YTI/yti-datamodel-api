package fi.vm.yti.datamodel.api.v2.repository;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.update.UpdateRequest;

import java.util.function.Consumer;

public abstract class BaseRepository {

    RDFConnection read;
    RDFConnection write;
    RDFConnection sparql;
    RDFConnection update;

    protected BaseRepository(RDFConnection read, RDFConnection write) {
        this.read = read;
        this.write = write;
    }

    protected BaseRepository(RDFConnection read, RDFConnection write, RDFConnection sparql) {
        this.read = read;
        this.write = write;
        this.sparql = sparql;
    }

    protected BaseRepository(RDFConnection read, RDFConnection write, RDFConnection sparql, RDFConnection update) {
        this.read = read;
        this.write = write;
        this.sparql = sparql;
        this.update = update;
    }

    public Model fetch(String graph) {
        return read.fetch(graph);
    }

    public void put(String graph, Model model) {
        write.put(graph, model);
    }

    public boolean graphExists(String graph) {
        var askBuilder = new AskBuilder()
                .addGraph(NodeFactory.createURI(graph), "?s", "?p", "?o");
        return this.queryAsk(askBuilder.build());
    }

    public boolean resourceExistsInGraph(String graph, String resource) {
        var askBuilder = new AskBuilder()
                .addGraph(NodeFactory.createURI(graph),
                        NodeFactory.createURI(resource), "?p", "?o");
        return this.queryAsk(askBuilder.build());
    }

    public Model queryConstruct(Query query) {
        return sparql.queryConstruct(query);
    }

    public void querySelect(Query query, Consumer<QuerySolution> consumer) {
        sparql.querySelect(query, consumer);
    }

    public void querySelect(String query, Consumer<QuerySolution> consumer) {
        sparql.querySelect(query, consumer);
    }

    public boolean queryAsk(Query query) {
        return sparql.queryAsk(query);
    }

    public void queryUpdate(String query) {
        update.update(query);
    }

    public void queryUpdate(UpdateRequest query) {
        update.update(query);
    }
}