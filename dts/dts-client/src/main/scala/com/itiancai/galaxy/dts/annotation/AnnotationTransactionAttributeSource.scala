package com.itiancai.galaxy.dts.annotation

import java.lang.reflect.Method

import com.itiancai.galaxy.dts.config.TransactionManagementConfigUtils
import com.itiancai.galaxy.dts.interceptor.{TransactionAttribute, AbstractFallbackTransactionAttributeSource}
import org.springframework.stereotype.Component

import scala.collection.immutable.HashSet


@Component(TransactionManagementConfigUtils.ANNOTATION_TRAN_ATTRIBUTE_SOURCE_BEAN)
class AnnotationTransactionAttributeSource extends AbstractFallbackTransactionAttributeSource {

  val annotationParsers: Set[TransactionAnnotationParser] =
      HashSet[TransactionAnnotationParser](ActivityAnnotationParse, ActionAnnotationParse)

  override def findTransactionAttribute(specificMethod: Method): TransactionAttribute = {
    for (annotationParser <- annotationParsers) {
      val attr = annotationParser.parseTransactionAnnotation(specificMethod)
      if (attr.isDefined)
        return attr.get
    }
    null
  }


}
