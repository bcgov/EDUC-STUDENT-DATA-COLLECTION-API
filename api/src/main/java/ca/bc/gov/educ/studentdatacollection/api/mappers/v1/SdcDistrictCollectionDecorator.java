package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcDistrictCollection;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
public abstract class SdcDistrictCollectionDecorator implements SdcDistrictCollectionMapper {

  private final SdcDistrictCollectionMapper delegate;

  protected SdcDistrictCollectionDecorator(SdcDistrictCollectionMapper delegate) {
    this.delegate = delegate;
  }

  @Override
  public SdcDistrictCollection toStructureWithSubmissionSignatures(SdcDistrictCollectionEntity sdcDistrictCollectionEntity) {
    final var coll = this.delegate.toStructureWithSubmissionSignatures(sdcDistrictCollectionEntity);
    SdcDistrictCollectionSubmissionSignatureMapper submissiontMapper = SdcDistrictCollectionSubmissionSignatureMapper.mapper;
    coll.setSubmissionSignatures(new ArrayList<>());
    sdcDistrictCollectionEntity.getSdcDistrictCollectionSubmissionSignatureEntities().forEach(signature -> coll.getSubmissionSignatures().add(submissiontMapper.toStructure(signature)));
    return coll;
  }



}
