package org.fairgenomes.transformer.implementations.molgenisemx;

import org.fairgenomes.transformer.datastructures.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

public class ToEMX {


    FAIRGenomes fg;
    File outputFolder;
    static final String RN = "\n";
    public static final String PACKAGE_NAME = "fair-genomes";

    public ToEMX(FAIRGenomes fg, File outputFolder) throws Exception {
        this.fg = fg;
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }
        this.outputFolder = outputFolder;
    }


    public void go() throws IOException {

        /*
        MOLGENIS Commander installation script.
        Start here, keep adding stuff as we iterate over the data, and close at the very end.
         */
        FileWriter MCMDfw = new FileWriter(new File(outputFolder, "setup.sh"));
        BufferedWriter MCMDbw = new BufferedWriter(MCMDfw);

        /*
        Write package info
         */
        FileWriter fw = new FileWriter(new File(outputFolder, "sys_md_Package.tsv"));
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write("id\tlabel\tdescription" + RN);
        bw.write(PACKAGE_NAME + "\t" + fg.name + "\t" + fg.description + (fg.description.endsWith(".")?"":".") + " Version " + fg.version + " (" + fg.date + ")" + RN);
        bw.flush();
        bw.close();
        MCMDbw.write("mcmd import -p sys_md_Package.tsv" + RN);

        /*
        Write attribute metadata for lookups
         */
        for (Module m : fg.modules) {
            for (Element e : m.elements) {
                if(e.isLookup())
                {
                    String entityName = m.technicalName + "_" + e.technicalName;
                    String fileName = entityName + "_attributes.tsv";
                    fw = new FileWriter(new File(outputFolder, fileName));
                    bw = new BufferedWriter(fw);
                    bw.write("name\tentity\tlabel\tdataType\tidAttribute\tnillable" + RN);
                    bw.write("value\t" + entityName + "\t" + "Value (English)" + "\t" + "String" + "\t" + "TRUE" + "\t" + "FALSE" + RN);
                    bw.write("description\t" + entityName + "\t" + "Description (English)" + "\t" + "Text" + "\t" + "FALSE" + "\t" + "TRUE" + RN);
                    bw.write("codesystem\t" + entityName + "\t" + "The code system (e.g. ontology) this term belongs to" + "\t" + "String" + "\t" + "FALSE" + "\t" + "TRUE" + RN);
                    bw.write("code\t" + entityName + "\t" + "The code within the code system" + "\t" + "String" + "\t" + "FALSE" + "\t" + "TRUE" + RN);
                    bw.write("iri\t" + entityName + "\t" + "The Internationalized Resource Identifier for this term" + "\t" + "String" + "\t" + "FALSE" + "\t" + "TRUE" + RN);
                    bw.flush();
                    bw.close();
                    MCMDbw.write("mcmd import -p " + fileName + " --as attributes --in " + PACKAGE_NAME + RN);
                }
            }
        }

        /*
        Write the actual lookups
         */
        for (Module m : fg.modules) {
            for (Element e : m.elements) {
                if (e.isLookup()) {

                    /*
                    Copy the original and add NullFlavors
                     */
                    String entityName = m.technicalName + "_" + e.technicalName;
                    File targetFile = new File(outputFolder,entityName + ".tsv");

                    Path target = Paths.get(targetFile.getAbsolutePath());
                    Path original = e.lookup.srcFile.toPath();
                    Files.copy(original, target, StandardCopyOption.REPLACE_EXISTING);

                    fw = new FileWriter(targetFile,true);
                    bw = new BufferedWriter(fw);

                    HashMap<String, Lookup> ll = fg.lookupGlobalOptionsInstance.lookups;
                    for(String key: ll.keySet())
                    {
                        Lookup l = ll.get(key);
                        bw.write(l.value + "\t" + l.description + "\t" + l.codesystem + "\t" + l.code + "\t" + l.iri + RN);
                    }

                    bw.flush();
                    bw.close();

                    MCMDbw.write("mcmd import -p "+targetFile.getName()+" --as fair-genomes_"+entityName+" --in " + PACKAGE_NAME + RN);
                }
            }
        }

        /*
        Write model attributes for the modules (the actual tables that people use to enter data)
         */
        for (Module m : fg.modules) {
            String entityName = m.technicalName;
            String fileName = entityName + "_attributes.tsv";
            fw = new FileWriter(new File(outputFolder, fileName));
            bw = new BufferedWriter(fw);
            bw.write("name\tlabel\tdescription\tentity\tdataType\tidAttribute\tlabelAttribute\tvisible\tnillable\trefEntity" + RN);

            for (Element e : m.elements) {
                if(e.valueTypeEnum.equals(ValueType.UniqueID))
                {
                    bw.write(e.technicalName+"\t"+e.name+"\t"+e.description+"\t"+entityName+"\t"+e.valueTypeToEMX()+"\t"+"TRUE"+"\t"+"TRUE"+"\t"+"TRUE"+"\t"+"FALSE"+"\t"+e.lookupOrReferencetoEMX()+ RN);
                }
                else{
                    bw.write(e.technicalName+"\t"+e.name+"\t"+e.description+"\t"+entityName+"\t"+e.valueTypeToEMX()+"\t"+"FALSE"+"\t"+"FALSE"+"\t"+"TRUE"+"\t"+"TRUE"+"\t"+e.lookupOrReferencetoEMX()+ RN);
                }
            }
            bw.flush();
            bw.close();
            MCMDbw.write("mcmd import -p " + fileName + " --as attributes --in " + PACKAGE_NAME + RN);
        }

        /*
        Landing page
         */
        MCMDbw.write("mcmd import -p ../../misc/molgenis/other/sys_StaticContent.tsv -a add_update_existing" + RN);
        String[] imgs = new String[]{"analysis", "lookups", "clinical", "informedconsentform", "individualconsent", "contribute", "info", "material", "personal", "samplepreparation", "sequencing", "study", "fair_genomes_logo_notext", "fair_genomes_logo"};
        for(String img : imgs)
        {
            MCMDbw.write("mcmd add logo -p ../../misc/molgenis/img/"+img+".png" + RN);
        }

        /*
        Demo permissions
         */
        MCMDbw.write("mcmd make --role ANONYMOUS fair-genomes_EDITOR" + RN);
        MCMDbw.write("mcmd give anonymous view sys_md" + RN);


        /*
        Flush and close MOLGENIS Commander script
         */
        MCMDbw.flush();
        MCMDbw.close();

    }

}
