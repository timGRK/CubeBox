package com.cube.logic;

import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cube.exception.RespException;

public abstract class DefaultHttpProcess implements HttpProcess {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultHttpProcess.class);

    @Override
    public void execute(ChannelHandlerContext ctx, FullHttpRequest req) {
        FullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        Map<String, List<String>> params = null;
        HttpPostRequestDecoder decoder = null;
        try {
            if (req.getMethod() == HttpMethod.GET) {
                QueryStringDecoder queryStringDecoder = new QueryStringDecoder(req.getUri());
                params = queryStringDecoder.parameters();
            } else if (req.getMethod() == HttpMethod.POST) {
                decoder = new HttpPostRequestDecoder(req);
            } else {
                throw new Exception("HTTP method 不支持:" + req.getMethod().name());
            }
            // 执行业务逻辑
            doExcute(ctx, req, resp, params, decoder);
            sendResp(ctx, resp);
        } catch (RespException e) {
            LOG.info("RespException 返回");
        } catch (Exception e) {
            LOG.error("执行HTTP处理", e);
            if(ctx.channel().isWritable()){
                resp.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                sendResp(ctx, resp);
            }else{
                if(resp.refCnt() >= 1){
                    resp.release();
                }
                ctx.channel().close();
            }
        } 
    }

    protected abstract void doExcute(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse resp,
            Map<String, List<String>> params, HttpPostRequestDecoder decoder) throws Exception;

    public String parseData(InterfaceHttpData data) throws IOException {
        if (data == null) {
            return null;
        }
        if (data.getHttpDataType() == HttpDataType.Attribute) {
            Attribute attribute = (Attribute) data;
            String v = attribute.getValue();
            return v;
        }
        return null;
    }

    public InterfaceHttpData getHttpData(String name, HttpPostRequestDecoder decoder) {
        if (decoder == null) {
            return null;
        }
        return decoder.getBodyHttpData(name);
    }

    public List<InterfaceHttpData> getHttpDatas(String name, HttpPostRequestDecoder decoder) {
        if (decoder == null) {
            return Collections.emptyList();
        }
        List<InterfaceHttpData> retLst = decoder.getBodyHttpDatas(name);
        if (retLst == null) {
            return Collections.emptyList();
        }
        return retLst;
    }

    public String getParam(String name, Map<String, List<String>> params) {
        if (params == null) {
            return null;
        }
        List<String> retLst = params.get(name);
        if (retLst == null) {
            return null;
        }
        if (!retLst.isEmpty()) {
            return retLst.get(0);
        } else {
            return null;
        }
    }

    public List<String> getParams(String name, Map<String, List<String>> params) {
        if (params == null) {
            return null;
        }
        List<String> retLst = params.get(name);
        if (retLst == null) {
            return Collections.emptyList();
        }
        return retLst;
    }

    public void sendResp(ChannelHandlerContext ctx, FullHttpResponse resp) {
        if (resp.getStatus().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(resp.getStatus().toString(), CharsetUtil.UTF_8);
            resp.content().writeBytes(buf);
            buf.release();
        }

        if (ctx.channel().isWritable()) {
            setContentLength(resp, resp.content().readableBytes());
            ctx.channel().writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.channel().close();
        }

    }

    public HttpMethod method(FullHttpRequest req) {
        return req.getMethod();
    }

    protected void sendTimeout(FullHttpResponse resp) {
    	//超时
        resp.content().writeBytes(("503").getBytes());
        resp.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
    }

    protected void sendUnableConn(FullHttpResponse resp) {
    	//无有效连接
        resp.content().writeBytes(("502").getBytes());
        resp.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
    }

    protected void sendParamsError(String msg, FullHttpResponse resp) {
        resp.content().writeBytes(msg.getBytes());
        resp.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
    }
}
