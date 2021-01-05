package org.fairgenomes.transformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.fairgenomes.transformer.datastructures.FAIRGenomes;
import org.fairgenomes.transformer.implementations.artdecor.ToARTDECOR;
import org.fairgenomes.transformer.implementations.markdown.ToMD;
import org.fairgenomes.transformer.implementations.molgenisemx.ToEMX;
import org.fairgenomes.transformer.implementations.rdfowl.ToOWL;

import java.io.*;

public class Transformer {

    private File inputF;

    public Transformer() {
        this.inputF = new File("fair-genomes.yml");
    }

    public void generateResources() throws Exception {

        System.out.println("Parsing FAIR Genomes YAML...");
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        FAIRGenomes fg = mapper.readValue(inputF, FAIRGenomes.class);

        System.out.println("Loading lookups and value types...");
        fg.loadLookupGlobalOptions();
        fg.parseElementValueTypes();
        fg.loadElementLookups();
        fg.parseOntologyReferences();
        fg.createElementTechnicalNames();

        System.out.println("Transforming into other representations...");
        File outputs = new File("transformation-output");
        new ToMD(fg, new File(outputs, "markdown")).go();
        new ToEMX(fg, new File(outputs, "molgenis-emx")).go();
        new ToOWL(fg, new File(outputs, "rdf-owl")).go();
        new ToARTDECOR(fg, new File(outputs, "art-decor")).go();


    }
}