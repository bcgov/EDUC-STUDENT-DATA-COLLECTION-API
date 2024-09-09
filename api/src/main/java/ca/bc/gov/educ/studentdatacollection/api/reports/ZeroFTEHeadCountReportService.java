package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.helpers.ZeroFTEHeadcountHelper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.*;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
public class ZeroFTEHeadCountReportService extends BaseReportGenerationService<ZeroFTEHeadcountResult>{
    protected static final String HEADER = "FTEREASONS";
    protected static final String FOOTER = "TOTAL";
    private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
    private final ZeroFTEHeadcountHelper zeroFTEHeadcountHelper;
    private JasperReport ineligibleFteHeadcountReport;

    private List<ZeroFTEHeadcountResult> fteReasonHeadcounts = new ArrayList<>();
    private Map<String, String> fteReasons = new HashMap<>();

    public ZeroFTEHeadCountReportService(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils, SdcDistrictCollectionRepository sdcDistrictCollectionRepository, ZeroFTEHeadcountHelper zeroFTEHeadcountHelper) {
        super(restUtils, sdcSchoolCollectionRepository);
        this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
        this.sdcDistrictCollectionRepository = sdcDistrictCollectionRepository;
        this.zeroFTEHeadcountHelper = zeroFTEHeadcountHelper;
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
            InputStream inputHeadcount = getClass().getResourceAsStream("/reports/zeroFTEHeadcount.jrxml");
            ineligibleFteHeadcountReport = JasperCompileManager.compileReport(inputHeadcount);
        } catch (JRException e) {
            throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper report(ZeroFTEHeadCountReportService) has failed :: " + e.getMessage());
        }
    }

    /**
     * Executor method that generates the PDF report.
     * @param collectionID Active CollectionID
     * @return
     */
    public DownloadableReportResponse generateZeroFTEHeadcountReport(UUID collectionID){
            try {
                Optional<SdcDistrictCollectionEntity> sdcDistrictCollectionEntityOptional = sdcDistrictCollectionRepository.findById(collectionID);
                SdcDistrictCollectionEntity sdcDistrictCollectionEntity = sdcDistrictCollectionEntityOptional.orElseThrow(() ->
                        new EntityNotFoundException(SdcDistrictCollectionEntity.class, "Collection ID: " + collectionID));
                fteReasons = zeroFTEHeadcountHelper.getZeroFTEReasonCodes();
                fteReasonHeadcounts = sdcSchoolCollectionStudentRepository.getZeroFTEHeadcountsBySdcDistrictCollectionId(sdcDistrictCollectionEntity.getSdcDistrictCollectionID());
                return generateJasperReport(convertToGradeEnrollmentProgramReportJSONStringDistrict(fteReasonHeadcounts, sdcDistrictCollectionEntity), ineligibleFteHeadcountReport, ReportTypeCode.DIS_ZERO_FTE_SUMMARY);
            } catch (JsonProcessingException e) {
                log.error("Exception occurred while writing PDF report for district Zero FTE summary :: " + e.getMessage());
                throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for district Zero FTE summary :: " + e.getMessage());
            }
    }

    /**
     * Helper method to prepare data for jasper template.
     * @param zeroFTEHeadcountResults List of ZeroFTEHeadcountResult
     * @param sdcDistrictCollection SdcDistrictCollectionEntity
     * @return Node
     * @throws JsonProcessingException Data transformation exception
     */
    private String convertToGradeEnrollmentProgramReportJSONStringDistrict(List<ZeroFTEHeadcountResult> zeroFTEHeadcountResults, SdcDistrictCollectionEntity sdcDistrictCollection) throws JsonProcessingException {
        HeadcountNode mainNode = new HeadcountNode();
        HeadcountReportNode reportNode = new HeadcountReportNode();
        setReportTombstoneValuesDis(sdcDistrictCollection, reportNode);
        var nodeMap = generateNodeMap(true);
        //This decorates the row of header
        decorateRowHeader(nodeMap);
        //Prepare data table
        zeroFTEHeadcountResults.forEach(fteReasonHeadcountResult -> setRowValues(nodeMap, fteReasonHeadcountResult));
        //Prepare the total(footer) row
        setRowTotal(nodeMap, zeroFTEHeadcountResults);
        //Adds all to report node.
        reportNode.setPrograms(nodeMap.values().stream().sorted(Comparator.comparing(o -> Integer.parseInt(o.getSequence()))).toList());
        mainNode.setReport(reportNode);
        return objectWriter.writeValueAsString(mainNode);
    }

    /**
     * This method helps in preparing the nodes for header, datatable and footer.
     * @return Map of node key and IneligibleFteHeadcountChildNode
     */
    public HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH){
        HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
        //Header node
        addValuesForSectionToMap(nodeMap, HEADER, "Non-Funded Students", "00");
        int sequencePrefix = 10;
        //Iterate over the reasons and generate node
        for (Map.Entry<String, String> entry: fteReasons.entrySet()) {
            int finalSequencePrefix = sequencePrefix;
            addValuesForSectionToMap(nodeMap, entry.getKey(), entry.getValue(), String.valueOf(finalSequencePrefix));
            sequencePrefix += 10;
        }
        //Footer node
        addValuesForSectionToMap(nodeMap, FOOTER, "Total Non-Funded Students",  String.valueOf(sequencePrefix));
        return nodeMap;
    }

    /**
     * This method helps in preparing the header node.
     *
     * @param nodeMap Map of report nodes
     * @param sectionPrefix Node key
     * @param sectionTitle Node Description (Display)
     * @param sequencePrefix Sequence
     */
    private void addValuesForSectionToMap(HashMap<String, HeadcountChildNode> nodeMap,  String sectionPrefix, String sectionTitle, String sequencePrefix){
        if (sectionPrefix.equals(HEADER) || sectionPrefix.equals(FOOTER)) {
            nodeMap.put(sectionPrefix, new ZeroFTEHeadcountChildNode(sectionTitle, "true", sequencePrefix + "0"));
        } else {
            nodeMap.put(sectionPrefix, new ZeroFTEHeadcountChildNode(sectionTitle, "false", sequencePrefix + "0"));
        }
    }

    /**
     * This method helps in decorating the node values of header and update report nodemap
     * @param nodeMap Map of report nodes
     */
    private void decorateRowHeader(HashMap<String, HeadcountChildNode> nodeMap){
        ZeroFTEHeadcountChildNode headerNode = (ZeroFTEHeadcountChildNode)nodeMap.getOrDefault(HEADER, new ZeroFTEHeadcountChildNode());
            for(String gradeCode: zeroFTEHeadcountHelper.getGradeCodesForDistricts()){
                headerNode.setValueForGrade(gradeCode, "");
                headerNode.setValueTotal("");
            }
            nodeMap.put(HEADER, headerNode);
    }

    /**
     *  This method helps in preparing the node values for datatable i.e. FTE Reasons and update report nodemap
     *
     * @param nodeMap Map of report nodes
     * @param fteHeadCountResult
     */
    public void setRowValues(HashMap<String, HeadcountChildNode> nodeMap, ZeroFTEHeadcountResult fteHeadCountResult){
        for (ZeroFTEHeadcountResult fteReasonHeadcount : fteReasonHeadcounts) {
            ZeroFTEHeadcountChildNode fteNode = (ZeroFTEHeadcountChildNode)nodeMap.getOrDefault(fteReasonHeadcount.getFteZeroReasonCode(), new ZeroFTEHeadcountChildNode());
            for(String gradeCode: zeroFTEHeadcountHelper.getGradeCodesForDistricts()){
                fteNode.setValueForGrade(gradeCode, zeroFTEHeadcountHelper.getHeadCountValue(fteHeadCountResult, gradeCode));
                fteNode.setValueTotal(fteHeadCountResult.getAllLevels());
            }
            nodeMap.put(fteReasonHeadcount.getFteZeroReasonCode(), fteNode);
        }
    }

    /**
     * This method helps in preparing the grade-wise total and update report nodemap
     * @param nodeMap
     * @param fteHeadCountResults
     */
    public void setRowTotal(Map<String, HeadcountChildNode> nodeMap, List<ZeroFTEHeadcountResult> fteHeadCountResults){
        //Populate total node
        ZeroFTEHeadcountChildNode totalFteNode = (ZeroFTEHeadcountChildNode)nodeMap.getOrDefault(FOOTER, new ZeroFTEHeadcountChildNode());
        BigDecimal sectionTotal = BigDecimal.ZERO;
        for (String gradeCode : zeroFTEHeadcountHelper.getGradeCodesForDistricts()) {
            int totalHeadcountPerGrade = fteHeadCountResults.stream().mapToInt(result ->  Integer.parseInt(zeroFTEHeadcountHelper.getHeadCountValue(result, gradeCode))).sum();
            totalFteNode.setValueForGrade(gradeCode, String.valueOf(totalHeadcountPerGrade));
            sectionTotal = sectionTotal.add(new BigDecimal(totalHeadcountPerGrade));
        }
        totalFteNode.setValueTotal(String.valueOf(sectionTotal));
        nodeMap.put(FOOTER, totalFteNode);
    }

}
