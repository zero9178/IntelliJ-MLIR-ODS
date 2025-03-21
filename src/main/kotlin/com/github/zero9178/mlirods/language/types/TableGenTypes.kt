package com.github.zero9178.mlirods.language.types

import com.github.zero9178.mlirods.language.psi.TableGenFieldScopeNode

sealed class TableGenType

object TableGenIntType : TableGenType()

object TableGenBitType : TableGenType()

object TableGenStringType : TableGenType()

object TableGenDagType : TableGenType()

class TableGenBitsType(val size: Int?) : TableGenType()

class TableGenListType(val elementType: TableGenType) : TableGenType()

class TableGenRecordType(val record: TableGenFieldScopeNode) : TableGenType()

object TableGenUnknownType : TableGenType()
