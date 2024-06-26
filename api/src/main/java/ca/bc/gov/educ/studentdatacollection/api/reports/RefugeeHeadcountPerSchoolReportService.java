package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.RefugeeHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.HeadcountChildNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;

@Service
@Slf4j
public class RefugeeHeadcountPerSchoolReportService extends BaseReportGenerationService<RefugeeHeadcountResult> {

    private JasperReport refugeeHeadcountReport;

    protected RefugeeHeadcountPerSchoolReportService(RestUtils restUtils) {
        super(restUtils);
    }

    @PostConstruct
    public void init() {
        ApplicationProperties.bgTask.execute(this::initialize);
    }
    private void initialize() {
        this.compileJasperReports();
    }
    private void compileJasperReports(){
        try {
            InputStream inputHeadcount = getClass().getResourceAsStream("/reports/refugeeHeadcountsPerSchool.jrxml");
            refugeeHeadcountReport = JasperCompileManager.compileReport(inputHeadcount);
        } catch (JRException e) {
            throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
        }
    }

    protected HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH) {
        return null;
    }

    protected void setValueForGrade(HashMap<String, HeadcountChildNode> nodeMap, RefugeeHeadcountResult gradeResult) {

    }
}
