package eu.wdaqua.component.opentapioca.ned;

import java.net.URI;

/**
 * data object to be initialized by the OpenTapioca service response
 */
public class FoundWikidataResource {
    public int begin;
    public int end;
    public double score;
    public URI resource;

    public FoundWikidataResource(//
                                 int begin, //
                                 int end, //
                                 double score, //
                                 URI resource) {

        this.begin = begin;
        this.end = end;
        this.score = score;
        this.resource = resource;
    }

    public int getBegin() {
        return begin;
    }

    public int getEnd() {
        return end;
    }

    public double getScore() {
        return score;
    }

    public URI getResource() {
        return resource;
    }
}
