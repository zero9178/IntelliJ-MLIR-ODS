package com.github.zero9178.mlirods.language.psi

/**
 * Enumeration of the bang operators parsed as a generic 'bang_operator_value_node'.
 *
 * This is the set of bang operators known to TableGen, minus those that have their own dedicated token and PSI node and
 * are therefore parsed specially.
 *
 * The [operatorName] is the full operator token text including the leading '!'. [arity] is the range of allowed operand
 * counts (the values inside the parentheses; a leading '<type>' is not an operand). Operators whose arguments are folded
 * left-associatively (e.g. '!add') accept any number of operands greater than or equal to two.
 */
enum class TableGenBangOperator(val operatorName: String, val arity: IntRange) {
    // Comparison.
    EQ("!eq", 2..2),
    NE("!ne", 2..2),
    LE("!le", 2..2),
    LT("!lt", 2..2),
    GE("!ge", 2..2),
    GT("!gt", 2..2),

    // Control flow.
    IF("!if", 3..3),

    // Type and value queries.
    ISA("!isa", 1..1),
    EXISTS("!exists", 1..1),
    INITIALIZED("!initialized", 1..1),
    INSTANCES("!instances", 0..1),
    REPR("!repr", 1..1),

    // Arithmetic and bitwise.
    ADD("!add", 2..Int.MAX_VALUE),
    SUB("!sub", 2..2),
    MUL("!mul", 2..Int.MAX_VALUE),
    DIV("!div", 2..2),
    NOT("!not", 1..1),
    LOGTWO("!logtwo", 1..1),
    AND("!and", 2..Int.MAX_VALUE),
    OR("!or", 2..Int.MAX_VALUE),
    XOR("!xor", 2..Int.MAX_VALUE),
    SHL("!shl", 2..2),
    SRA("!sra", 2..2),
    SRL("!srl", 2..2),

    // List operations.
    HEAD("!head", 1..1),
    TAIL("!tail", 1..1),
    SIZE("!size", 1..1),
    EMPTY("!empty", 1..1),
    RANGE("!range", 1..3),
    LISTCONCAT("!listconcat", 2..Int.MAX_VALUE),
    LISTFLATTEN("!listflatten", 1..1),
    LISTSPLAT("!listsplat", 2..2),
    LISTREMOVE("!listremove", 2..2),
    INTERLEAVE("!interleave", 2..2),

    // String operations.
    STRCONCAT("!strconcat", 2..Int.MAX_VALUE),
    SUBST("!subst", 3..3),
    SUBSTR("!substr", 2..3),
    FIND("!find", 2..3),
    TOLOWER("!tolower", 1..1),
    TOUPPER("!toupper", 1..1),
    MATCH("!match", 2..2),

    // DAG operations.
    DAG("!dag", 3..3),
    CON("!con", 2..Int.MAX_VALUE),
    GETDAGOP("!getdagop", 1..1),
    SETDAGOP("!setdagop", 2..2),
    GETDAGOPNAME("!getdagopname", 1..1),
    SETDAGOPNAME("!setdagopname", 2..2),
    GETDAGARG("!getdagarg", 2..2),
    SETDAGARG("!setdagarg", 3..3),
    GETDAGNAME("!getdagname", 2..2),
    SETDAGNAME("!setdagname", 3..3),
    ;

    companion object {
        private val byOperatorName = entries.associateBy(TableGenBangOperator::operatorName)

        /**
         * Returns the [TableGenBangOperator] with the given full operator token text (e.g. '!div'),
         * or null if no such bang operator exists.
         */
        fun fromOperatorName(operatorName: String): TableGenBangOperator? = byOperatorName[operatorName]
    }
}
