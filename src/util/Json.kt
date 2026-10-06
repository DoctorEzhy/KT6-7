package util

class JsonException(message: String) : Exception(message)

object Json {

    fun parse(text: String): Any? {
        val parser = JsonParser(text)
        val value = parser.parseValue(0)
        parser.skipWhitespace()
        if (!parser.atEnd()) throw JsonException("лишние символы после JSON")
        return value
    }

    @Suppress("UNCHECKED_CAST")
    fun parseObject(text: String): Map<String, Any?> =
        parse(text) as? Map<String, Any?> ?: throw JsonException("ожидался JSON-объект { ... }")

    fun stringify(value: Any?, level: Int = 0): String {
        val inner = "  ".repeat(level + 1)
        val outer = "  ".repeat(level)
        return when (value) {
            null -> "null"
            is String -> quote(value)
            is Boolean, is Number -> value.toString()
            is Map<*, *> ->
                if (value.isEmpty()) "{}"
                else value.entries.joinToString(",\n", "{\n", "\n$outer}") {
                    inner + quote(it.key.toString()) + ": " + stringify(it.value, level + 1)
                }
            is List<*> ->
                if (value.isEmpty()) "[]"
                else value.joinToString(",\n", "[\n", "\n$outer]") {
                    inner + stringify(it, level + 1)
                }
            else -> throw IllegalArgumentException("Нельзя превратить в JSON: ${value::class}")
        }
    }

    fun quote(text: String): String {
        val sb = StringBuilder("\"")
        for (c in text) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c < ' ') sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        return sb.append('"').toString()
    }
}

private class JsonParser(private val s: String) {
    private var i = 0

    fun atEnd(): Boolean = i >= s.length

    fun skipWhitespace() {
        while (i < s.length && s[i].isWhitespace()) i++
    }

    private fun peek(): Char {
        if (atEnd()) throw JsonException("неожиданный конец JSON")
        return s[i]
    }

    private fun expect(c: Char) {
        if (peek() != c) throw JsonException("ожидался символ '$c' на позиции $i")
        i++
    }

    fun parseValue(depth: Int): Any? {
        if (depth > 50) throw JsonException("слишком глубокая вложенность")
        skipWhitespace()
        return when (peek()) {
            '{' -> parseObject(depth)
            '[' -> parseArray(depth)
            '"' -> parseString()
            't' -> literal("true", true)
            'f' -> literal("false", false)
            'n' -> literal("null", null)
            else -> parseNumber()
        }
    }

    private fun literal(word: String, value: Any?): Any? {
        if (!s.startsWith(word, i)) throw JsonException("ожидалось $word на позиции $i")
        i += word.length
        return value
    }

    private fun parseObject(depth: Int): Map<String, Any?> {
        val map = LinkedHashMap<String, Any?>()
        i++
        skipWhitespace()
        if (peek() == '}') {
            i++
            return map
        }
        while (true) {
            skipWhitespace()
            if (peek() != '"') throw JsonException("ожидался ключ в кавычках на позиции $i")
            val key = parseString()
            skipWhitespace()
            expect(':')
            map[key] = parseValue(depth + 1)
            skipWhitespace()
            when (peek()) {
                ',' -> i++
                '}' -> {
                    i++
                    return map
                }
                else -> throw JsonException("ожидалась ',' или '}' на позиции $i")
            }
        }
    }

    private fun parseArray(depth: Int): List<Any?> {
        val list = mutableListOf<Any?>()
        i++
        skipWhitespace()
        if (peek() == ']') {
            i++
            return list
        }
        while (true) {
            list.add(parseValue(depth + 1))
            skipWhitespace()
            when (peek()) {
                ',' -> i++
                ']' -> {
                    i++
                    return list
                }
                else -> throw JsonException("ожидалась ',' или ']' на позиции $i")
            }
        }
    }

    private fun parseString(): String {
        i++
        val sb = StringBuilder()
        while (true) {
            if (atEnd()) throw JsonException("строка не закрыта")
            val c = s[i++]
            when (c) {
                '"' -> return sb.toString()
                '\\' -> {
                    if (atEnd()) throw JsonException("строка оборвана после '\\'")
                    when (val e = s[i++]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'f' -> sb.append('\u000C')
                        'u' -> {
                            if (i + 4 > s.length) throw JsonException("неверная unicode-последовательность")
                            val code = s.substring(i, i + 4).toIntOrNull(16)
                                ?: throw JsonException("неверная unicode-последовательность")
                            sb.append(code.toChar())
                            i += 4
                        }
                        else -> throw JsonException("неизвестная escape-последовательность: \\$e")
                    }
                }
                else -> sb.append(c)
            }
        }
    }

    private fun parseNumber(): Any {
        val start = i
        while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
        val text = s.substring(start, i)
        if (text.isEmpty()) throw JsonException("неожиданный символ '${s[start]}' на позиции $start")
        return text.toLongOrNull() ?: text.toDoubleOrNull() ?: throw JsonException("неверное число: $text")
    }
}