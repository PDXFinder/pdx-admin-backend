package org.pdxfinder;

import org.pdxfinder.utils.reader.Reader;
import org.pdxfinder.utils.reader.TableSetCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MissingMappingService {

    Reader reader;
    TableSetCleaner tableSetCleaner;
    MappingService mappingService;

    @Value("${data-dir}")
    private String rootDir;

    private Logger log = LoggerFactory.getLogger(MissingMappingService.class);

    MappingContainer missingMappingsContainer;

    @Autowired
    public MissingMappingService(MappingService mappingService) {
        this.mappingService = mappingService;
        reader = new Reader();
        tableSetCleaner = new TableSetCleaner();
        missingMappingsContainer = new MappingContainer();

    }


    public MappingContainer getMissingMappings() {

        missingMappingsContainer = new MappingContainer();
        List<Path> subfolders = getProviderDirs();
        readDataFromTemplates(subfolders);

        return missingMappingsContainer;
    }

    public void readDataFromTemplates(List<Path> folders) {

        PathMatcher metadataFile = FileSystems.getDefault().getPathMatcher("glob:**{metadata-sam,metadata-load}*.tsv");
        for (Path p : folders) {
            Map<String, Table> metaDataTemplate = reader.readAllTsvFilesIn(p, metadataFile);
            metaDataTemplate = tableSetCleaner.cleanPdxTables(metaDataTemplate);
            getDiagnosisAttributesFromTemplate(metaDataTemplate);
        }
    }

    public List<Path> getProviderDirs() {

        Path updogDirectory = Paths.get(
                rootDir,
                "/data/UPDOG");

        List<Path> subfolders;
        try {
            subfolders = Files.walk(updogDirectory, 1)
                    .filter(p -> Files.isDirectory(p) && !p.equals(updogDirectory))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error opening updog dir");
            return new ArrayList<>();
        }
        return subfolders;
    }

    public void getDiagnosisAttributesFromTemplate(Map<String, Table> tables) {

        Table loaderTable = tables.get("metadata-loader.tsv");
        Row loaderRow = loaderTable.row(0);
        String dataSource = loaderRow.getString("abbreviation");

        Table sampleTable = tables.get("metadata-sample.tsv");

        for (Row row : sampleTable) {

            String primarySiteName = row.getString("primary_site");
            String diagnosis = row.getString("diagnosis");
            String tumorTypeName = row.getString("tumour_type");

            MappingEntity mappingEntity = mappingService.getDiagnosisMapping(dataSource,diagnosis,primarySiteName, tumorTypeName);

            if(mappingEntity == null){
                MappingEntity newUnmappedEntity = mappingService.saveUnmappedDiagnosis(dataSource, diagnosis, primarySiteName, tumorTypeName);
                if(newUnmappedEntity != null)
                    missingMappingsContainer.addEntity(newUnmappedEntity);
            }
        }
    }
}