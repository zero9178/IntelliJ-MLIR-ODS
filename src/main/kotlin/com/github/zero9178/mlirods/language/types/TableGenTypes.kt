package com.github.zero9178.mlirods.language.types

sealed class TableGenType

object TableGenIntType : TableGenType()

object TableGenBitType : TableGenType()

object TableGenStringType : TableGenType()

object TableGenDagType : TableGenType()

class TableGenBitsType(val size: Int?) : TableGenType()

class TableGenListType(val elementType: TableGenType) : TableGenType()

class TableGenClassType(val className: String) : TableGenType()

class TableGenErrorType(val message: String) : TableGenType()

object TableGenUnknownType : TableGenType()
