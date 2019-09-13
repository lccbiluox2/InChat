<!DOCTYPE html>
<html>
<#include "../common/header.ftl">

<script src="http://code.jquery.com/jquery-2.1.4.min.js"></script>
<script src='/susu/js/newChat.js'></script>

<style type="text/css">
    .infinite-scroll-preloader {
        margin-top:-20px;
    }
    .zoomImage {
                background-image:url(/susu/image/image-back2.jpg);
                background-repeat:no-repeat;
                background-size:100% 100%;
                -moz-background-size:100% 100%;
                }
</style>
<link rel="stylesheet" type="text/css" href="/susu/css/allchat.css"/>
<body>
<div class="page">
    <header class="bar bar-nav">
        <a class="button button-link button-nav pull-left" href="/susu/su/home" data-transition='slide-out'>
            <span class="icon icon-left"></span>
        </a>
        <h1 class="title">全体用户</h1>
    </header>
    <input hidden id="userName" value="${userName!''}">
    <div class="content infinite-scroll zoomImage" style="height: 70%;width:100%;background:url(/susu/image/image-back2.jpg);background-size:100% 100%;">
        <div class="chat"   >

        </div>
    </div>
    <div style="position: absolute;bottom: 0px;width: 100%;">
        <div class="row">
            <div class="col-85">
                <input class='msg' style="height: 50px;width: 100%;" id="message" >
            </div>
            <div class="col-35" style="width: auto" ><a href="#" class="button button-big button-fill button-success" style="width: 100%"  onclick="sendd()">发送</a></div>
        </div>
        <div class="col-120" style="width: 100%"><input class="button button-big button-fill button-success" style="width: 100%" type="file" id="file" value="图片"></div>

        <br/>
    </div>
</div>

<#include "../common/floor.ftl">
<script>
    var msg = '\n';
    <#list userMsgList as userMsg>
    msg = msg +'<div class="his">'+ '${userMsg.name!''}'+':'+'${userMsg.msg!''}' +'</div>';
    </#list>
    msg = msg+'<div class="his">--- 以上为历史记录 ---</div>';
</script>
</body>
</html>