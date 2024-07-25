package ca.bc.gov.educ.studentdatacollection.api.struct;

import ca.bc.gov.educ.studentdatacollection.api.struct.v1.BaseRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper=false)
public class CollectionSagaData extends BaseRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message="collectionID cannot be null.")
    private String existingCollectionID;

    @NotNull(message = "snapshot date cannot be null")
    private String newCollectionSnapshotDate;

    @NotNull(message = "submission due date cannot be null")
    private String newCollectionSubmissionDueDate;

    @NotNull(message = "duplication resolution due date cannot be null")
    private String newCollectionDuplicationResolutionDueDate;

    @NotNull(message = "sign off due date cannot be null")
    private String newCollectionSignOffDueDate;
}
