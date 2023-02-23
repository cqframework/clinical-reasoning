package org.opencds.cqf.cql.evaluator.fhir.util.r5;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.Attachment;
import org.hl7.fhir.r5.model.Library;

public class AttachmentUtil {

  public static String getCqlLocation(IBaseResource resource) {
    Library library = (Library) resource;
    for (Attachment attachment : library.getContent()) {
      if (attachment.hasContentType() &&
          attachment.getContentType().equals("text/cql") &&
          StringUtils.isNotBlank(attachment.getUrl())) {
        return attachment.getUrl();
      }
    }
    return null;
  }

  public static IBaseResource addData(IBaseResource resource, String text) {
    if (StringUtils.isNotBlank(text)) {
      Library library = (Library) resource;
      for (Attachment attachment : library.getContent()) {
        if (attachment.hasContentType() &&
            attachment.getContentType().equals("text/cql")) {
          attachment.setData(text.getBytes());
          library.addContent(attachment);
          return (IBaseResource) library;
        }
      }
    }
    return null;
  }
}

