package com.xhhao.comment.widget.iplocation;
import com.fasterxml.jackson.databind.JsonNode;
final class CommentNextIpLocationResponseParser {
    // Preserve the response until the request's district option can be applied.
    static String location(JsonNode node) { return node == null ? "" : node.toString(); }
}
