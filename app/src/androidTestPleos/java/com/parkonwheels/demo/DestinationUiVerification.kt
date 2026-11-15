package com.parkonwheels.demo

import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo as Node
import android.graphics.Rect

/** Operates the real installed Pleos activity. No injected model or vehicle signals. */
class DestinationUiVerification:Instrumentation() {
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    private fun all():List<Node> {
        uiAutomation.clearCache()
        val list=mutableListOf<Node>()
        fun visit(n:Node?){if(n==null)return;list.add(n);for(i in 0 until n.childCount)visit(n.getChild(i))}
        uiAutomation.windows.forEach{visit(it.root)};return list
    }
    private fun find(text:String)=all().firstOrNull{it.text?.toString()?.replace(" ","")==text.replace(" ","")}
    private fun clickNode(node:Node){var n:Node?=node;while(n!=null && !n.isClickable)n=n.parent;check(n?.performAction(Node.ACTION_CLICK)==true);Thread.sleep(450)}
    private fun click(text:String){val end=SystemClock.elapsedRealtime()+10000;while(SystemClock.elapsedRealtime()<end){find(text)?.let{clickNode(it);return};Thread.sleep(200)};error("Missing $text: ${all().mapNotNull{it.text}}")}
    private fun input(value:String){val node=all().firstOrNull{it.isEditable}?:error("No edit field");check(node.performAction(Node.ACTION_SET_TEXT,Bundle().apply{putCharSequence(Node.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value)}));Thread.sleep(350)}
    private fun capture(name:String){Thread.sleep(800);val f=java.io.File(targetContext.filesDir,"destination-$name.png");uiAutomation.takeScreenshot()!!.let{b->f.outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}}}
    private fun parents(){click("보호자");input("14");click("보호자 확인")}
    override fun onStart(){
        val info=uiAutomation.serviceInfo;info.flags=info.flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;uiAutomation.serviceInfo=info
        var ok=false;val evidence=StringBuilder()
        try{
            startActivitySync(Intent(targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Thread.sleep(2500)
            find("← 홈으로")?.let{clickNode(it)}
            find("입장하기")?.let{clickNode(it)}
            for((name,id) in listOf("해운대해수욕장" to "ocean","서울숲" to "dino","경복궁" to "magic","국립과천과학관" to "space")){
                parents()
                repeat(9){if(find("여행지에 맞는 테마 추천")==null){all().firstOrNull{it.isScrollable}?.performAction(Node.ACTION_SCROLL_FORWARD);Thread.sleep(350)}}
                val label=find("여행지에 맞는 테마 추천")?:error("recommendation setting missing")
                val r=Rect();label.getBoundsInScreen(r)
                val toggle=all().filter{it.isCheckable}.minByOrNull{val b=Rect();it.getBoundsInScreen(b);kotlin.math.abs(b.centerY()-r.centerY())}?:error("switch missing")
                if(!toggle.isChecked)clickNode(toggle)
                repeat(3){if(all().none{it.isEditable}){all().firstOrNull{it.isScrollable}?.performAction(Node.ACTION_SCROLL_FORWARD);Thread.sleep(300)}}
                input(name);capture("$id-input");click("여행지 적용")
                click("탐험");find("← 홈으로")?.let{clickNode(it)}
                check(all().any{it.text?.contains("오늘의 여행 추천")==true || it.contentDescription?.contains("오늘의 여행 추천")==true})
                capture("$id-home");evidence.append("PASS real Pleos UI manual destination $name -> recommendation ($id)\n")
            }
            ok=true
        }catch(t:Throwable){capture("failure");evidence.append(t.stackTraceToString())}
        finish(if(ok)android.app.Activity.RESULT_OK else android.app.Activity.RESULT_CANCELED,Bundle().apply{putString("stream",evidence.toString())})
    }
}
