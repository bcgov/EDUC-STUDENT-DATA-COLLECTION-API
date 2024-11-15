package ca.bc.gov.educ.studentdatacollection.api.struct;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateStudentSagaData {
    String sdcSchoolCollectionStudentID;
    String assignedPEN;
    String assignedStudentID;
    String dob;
    String sexCode;
    String genderCode;
    String usualFirstName;
    String usualMiddleNames;
    String usualLastName;
    String postalCode;
    String mincode;
    String localID;
    String gradeCode;
    String numberOfCourses;
    String collectionTypeCode;
    String collectionID;

}
