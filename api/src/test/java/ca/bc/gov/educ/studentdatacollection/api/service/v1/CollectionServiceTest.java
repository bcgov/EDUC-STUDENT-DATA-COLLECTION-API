package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CollectionServiceTest {

    @Mock
    private CollectionRepository collectionRepository;

    @InjectMocks
    private CollectionService collectionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetActiveCollection_NoActiveCollection_ShouldThrowException(){
        CollectionEntity mockCollectionEntity = new CollectionEntity();

        mockCollectionEntity.setCollectionID(UUID.randomUUID());
        mockCollectionEntity.setCollectionTypeCode("SEPTEMBER");
        mockCollectionEntity.setOpenDate(LocalDateTime.now().minusMonths(6));
        mockCollectionEntity.setCloseDate(LocalDateTime.now().minusMonths(3));

        collectionRepository.save(mockCollectionEntity);

        assertThrows(EntityNotFoundException.class, () -> {
            collectionService.getActiveCollection();
        });
    }
}
