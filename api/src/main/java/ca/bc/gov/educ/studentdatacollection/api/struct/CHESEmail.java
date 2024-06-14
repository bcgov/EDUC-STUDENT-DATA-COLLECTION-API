package ca.bc.gov.educ.studentdatacollection.api.struct;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CHESEmail {

  private String bodyType;
  private String body;
  private Integer delayTS;
  private String encoding;
  private String from;
  private String subject;
  private String priority;
  private List<String> to;
  private String tag;

  public List<String> getTo() {
    if (this.to == null) {
      this.to = new ArrayList<>();
    }
    return this.to;
  }
}
