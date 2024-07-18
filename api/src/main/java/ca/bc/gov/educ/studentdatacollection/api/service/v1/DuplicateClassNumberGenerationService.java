package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DuplicateClassLookup;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidParameterException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.UUID;

@Service
public class DuplicateClassNumberGenerationService {

    private EnumMap<DuplicateClassLookup, Integer> map;

    public DuplicateClassNumberGenerationService(){
        init();
    }

    public Integer generateDuplicateClassNumber(UUID sdcDuplicateStudentID, String facilityTypeCode, String schoolCategoryCode, String gradeCode){
        DuplicateClassLookup value = DuplicateClassLookup.getClassNumber(facilityTypeCode, schoolCategoryCode, gradeCode);
        if(value != null){
            return map.get(value);
        } else {
            throw new InvalidParameterException("Duplicate class number cannot be generated for duplicate student record: " + sdcDuplicateStudentID);
        }
    }

    @PostConstruct
    public void init(){
        map = new EnumMap<>(DuplicateClassLookup.class);

        map.put(DuplicateClassLookup.ENTRY1, 1);
        map.put(DuplicateClassLookup.ENTRY2, 2);
        map.put(DuplicateClassLookup.ENTRY3, 3);
        map.put(DuplicateClassLookup.ENTRY4, 4);
        map.put(DuplicateClassLookup.ENTRY5, 5);
        map.put(DuplicateClassLookup.ENTRY6, 6);
        map.put(DuplicateClassLookup.ENTRY7, 7);
        map.put(DuplicateClassLookup.ENTRY8, 8);
        map.put(DuplicateClassLookup.ENTRY9, 9);
        map.put(DuplicateClassLookup.ENTRY10, 10);
        map.put(DuplicateClassLookup.ENTRY11, 11);
    }

}
