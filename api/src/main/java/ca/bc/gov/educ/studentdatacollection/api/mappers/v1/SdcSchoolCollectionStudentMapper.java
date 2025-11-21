package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;


import ca.bc.gov.educ.studentdatacollection.api.mappers.UUIDMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionSLDHistoryStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentHistory;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentHistoryPagination;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentShallow;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = UUIDMapper.class)
@DecoratedWith(SdcSchoolCollectionStudentDecorator.class)
public interface SdcSchoolCollectionStudentMapper {

  SdcSchoolCollectionStudentMapper mapper = Mappers.getMapper(SdcSchoolCollectionStudentMapper.class);

  @Mapping(target = "sdcSchoolCollectionID", source = "sdcSchoolCollection.sdcSchoolCollectionID")
  @Mapping(target = "sdcDistrictCollectionID", source = "sdcSchoolCollection.sdcDistrictCollectionID")
  @Mapping(target = "schoolID", source = "sdcSchoolCollection.schoolID")
  SdcSchoolCollectionStudent toSdcSchoolStudent(SdcSchoolCollectionStudentEntity sdcSchoolStudentEntity);

  @Mapping(target = "sdcSchoolCollectionID", source = "sdcSchoolCollection.sdcSchoolCollectionID")
  @Mapping(target = "sdcDistrictCollectionID", source = "sdcSchoolCollection.sdcDistrictCollectionID")
  SdcSchoolCollectionStudent toSdcSchoolCollectionStudentWithValidationIssues(SdcSchoolCollectionStudentEntity sdcSchoolStudentEntity);

  @Mapping(target = "sdcSchoolCollectionID", source = "sdcSchoolCollection.sdcSchoolCollectionID")
  @Mapping(target = "sdcDistrictCollectionID", source = "sdcSchoolCollection.sdcDistrictCollectionID")
  @Mapping(target = "schoolID", source = "sdcSchoolCollection.schoolID")
  SdcSchoolCollectionStudent toSdcSchoolStudent(SdcSchoolCollectionStudentPaginationEntity sdcSchoolStudentEntity);

  SdcSchoolCollectionStudentHistory toSdcSchoolStudentHistory(SdcSchoolCollectionStudentHistoryEntity sdcSchoolStudentEntity);

  @Mapping(target = "sdcSchoolCollectionID", source = "sdcSchoolCollectionEntity.sdcSchoolCollectionID")
  @Mapping(target = "sdcDistrictCollectionID", source = "sdcSchoolCollectionEntity.sdcDistrictCollectionID")
  @Mapping(target = "schoolID", source = "sdcSchoolCollectionEntity.schoolID")
  @Mapping(target = "snapshotDate", source = "sdcSchoolCollectionEntity.collectionEntity.snapshotDate")
  SdcSchoolCollectionStudentHistoryPagination toSdcSchoolStudentHistoryPagination(SdcSchoolCollectionStudentHistoryPaginationEntity sdcSchoolStudentEntity);

  @Mapping(target = "sdcSchoolCollectionID", source = "sdcSchoolCollection.sdcSchoolCollectionID")
  @Mapping(target = "sdcDistrictCollectionID", source = "sdcSchoolCollection.sdcDistrictCollectionID")
  @Mapping(target = "schoolID", source = "sdcSchoolCollection.schoolID")
  SdcSchoolCollectionStudent toSdcSchoolCollectionStudentWithValidationIssues(SdcSchoolCollectionStudentPaginationEntity sdcSchoolStudentEntity);

  @Mapping(target = "schoolID", source = "sdcSchoolCollection.schoolID")
  SdcSchoolCollectionStudentShallow toSdcSchoolCollectionStudentShallow(SdcSchoolCollectionStudentPaginationShallowEntity sdcSchoolStudentEntity);

  @Mapping(target = "sdcSchoolCollectionID", source = "sdcSchoolCollection.sdcSchoolCollectionID")
  @Mapping(target = "sdcDistrictCollectionID", source = "sdcSchoolCollection.sdcDistrictCollectionID")
  @Mapping(target = "schoolID", source = "sdcSchoolCollection.schoolID")
  @Mapping(target = "snapshotDate", source = "sdcSchoolCollection.collectionEntity.snapshotDate")
  SdcSchoolCollectionSLDHistoryStudent toSLDHistoryStudent(SdcSchoolCollectionStudentPaginationEntity sdcSchoolStudentEntity);

  @Mapping(target = "sdcSchoolCollectionID", source = "sdcSchoolCollection.sdcSchoolCollectionID")
  @Mapping(target = "sdcDistrictCollectionID", source = "sdcSchoolCollection.sdcDistrictCollectionID")
  @Mapping(target = "schoolID", source = "sdcSchoolCollection.schoolID")
  @Mapping(target = "snapshotDate", source = "sdcSchoolCollection.collectionEntity.snapshotDate")
  SdcSchoolCollectionSLDHistoryStudent toSdcSchoolCollectionSLDHistoryStudent(SdcSchoolCollectionStudentPaginationEntity sdcSchoolStudentEntity);

  @Mapping(target = "sdcSchoolCollection.sdcSchoolCollectionID", source = "sdcSchoolCollectionID")
  SdcSchoolCollectionStudentEntity toSdcSchoolStudentEntity(SdcSchoolCollectionStudent sdcSchoolStudent);

  @Mapping(target = "sdcSchoolCollectionID", source = "sdcSchoolCollection.sdcSchoolCollectionID")
  @Mapping(target = "sdcSchoolCollectionEntity", source = "sdcSchoolCollection")
  SdcSchoolCollectionStudentLightEntity toSdcSchoolStudentLightEntity(SdcSchoolCollectionStudentEntity sdcSchoolStudentEntity);

  @Mapping(target = "sdcSchoolCollectionStudentID", source = "sdcSchoolCollectionStudentID")
  @Mapping(target = "sdcSchoolCollectionEntity.sdcSchoolCollectionID", source = "sdcSchoolCollectionID")
  SdcSchoolCollectionStudentLightEntity toSdcSchoolStudentLightEntityFromSdcStudent(SdcSchoolCollectionStudent sdcSchoolStudent);

  @Mapping(target = "sdcDistrictCollectionID", source = "sdcSchoolCollectionEntity.sdcDistrictCollectionID")
  @Mapping(target = "schoolID", source = "sdcSchoolCollectionEntity.schoolID")
  SdcSchoolCollectionStudent toSdcSchoolStudent(SdcSchoolCollectionStudentLightEntity sdcSchoolStudentEntity);

  @Mapping(target = "sdcSchoolCollection", source = "sdcSchoolCollectionEntity")
  SdcSchoolCollectionStudentEntity toSdcSchoolStudentEntity(SdcSchoolCollectionStudentLightEntity sdcSchoolStudent);
}
