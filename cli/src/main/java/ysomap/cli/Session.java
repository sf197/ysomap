package ysomap.cli;

import ysomap.bullets.Bullet;
import ysomap.cli.model.Status;
import ysomap.cli.utils.Printer;
import ysomap.common.annotation.Require;
import ysomap.common.exception.ArgumentsMissMatchException;
import ysomap.common.util.ColorStyle;
import ysomap.common.util.Logger;
import ysomap.core.serializer.Serializer;
import ysomap.core.serializer.SerializerFactory;
import ysomap.exploits.Exploit;
import ysomap.payloads.Payload;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author wh1t3P1g
 * @since 2021/6/14
 */
public class Session {

    private String uuid = UUID.randomUUID().toString();
    public Status status = new Status();

    public Exploit exploit;
    public Payload payload;
    public Bullet bullet;

    public Map<String, HashMap<String, Object>> settings = new HashMap<>();
    private Console console;
    private boolean isExploit = false;
    private boolean isEmpty = true;

    public Session(Console console) {
        this.console = console;
    }

    public void accept(){

    }

    public Status getStatus() {
        return status;
    }

    public String getUuid() {
        return uuid;
    }

    public boolean isExploit() {
        return isExploit;
    }

    public void setExploit(boolean exploit) {
        isExploit = exploit;
    }

    public void create(String type, Class<?> clazz) throws Exception{
        clean(type);
        if("exploit".equals(type)){
            exploit = (Exploit) clazz.newInstance();
            isExploit = true;
            settings.put("exploit", update(clazz));

            for(String field:settings.get("exploit").keySet()){
                String defaultValue = exploit.get(field);
                settings.get("exploit").put(field, defaultValue);
            }
        }else if("payload".equals(type)){
            payload = (Payload) clazz.newInstance();
            if(isExploit && exploit.has("payloadName")){
                exploit.set("payloadName", clazz.getSimpleName());
            }
            settings.put("payload", update(clazz));
        }else if("bullet".equals(type)){
            bullet = (Bullet) clazz.newInstance();
            settings.put("bullet", update(clazz));
            for(String field:settings.get("bullet").keySet()){
                String defaultValue = bullet.get(field);
                settings.get("bullet").put(field, defaultValue);
            }
        }
        if(isEmpty) isEmpty = false;
        Logger.success("Created a new "+type+"("+clazz.getSimpleName()+")");
    }

    public void clean(String type){
        if("exploit".equals(type)){
            status.removePrompt("payload");
            status.removePrompt("bullet");
            // clean settings
            settings.put("exploit", new HashMap<>());
            settings.put("payload", new HashMap<>());
            settings.put("bullet", new HashMap<>());
            exploit = null;
            payload = null;
            bullet = null;
        }else if("payload".equals(type)) {
            status.removePrompt("bullet");
            // clean settings
            settings.put("payload", new HashMap<>());
            settings.put("bullet", new HashMap<>());
            payload = null;
            bullet = null;
        }else if("bullet".equals(type)){
            settings.put("bullet", new HashMap<>());
            bullet = null;
        }
    }

    public HashMap<String, Object> update(Class<?> clazz){
        HashMap<String, Object> settings = new HashMap<>();
        if(clazz == null)return settings;
        Set<String> fields = Require.Utils.getFieldNames(clazz);
        for(String field:fields){
            settings.put(field, null);
        }
        return settings;
    }

    public void setValue(String key, String value) throws ArgumentsMissMatchException {
        if("encoder".equals(key) && payload != null){
            Serializer serializer = payload.getSerializer();
            serializer.setEncoder(value);
            return;
        }else if("output".equals(key) && payload != null){
            Serializer serializer = payload.getSerializer();
            serializer.setOutputType(value);
            return;
        }

        if(exploit != null && exploit.has(key)){
            try {
                exploit.set(key, value);
                settings.get("exploit").put(key, value);
                return;
            } catch (Exception e) {
                // do nothing
            }
        }

        if(bullet != null && bullet.has(key)){
            try {
                bullet.set(key, value);
                settings.get("bullet").put(key, value);
                return;
            } catch (Exception e) {
                // do nothing
            }
        }

        throw new ArgumentsMissMatchException("no key("+ ColorStyle.makeWordRed(key)+") found");
    }

    public void printCandidates(String type){
        if("payloads".equals(type) && isExploit && exploit != null){
            Class<?> clazz = exploit.getClass();
            if(payload != null){
                Logger.normal("Current Payload: "+ColorStyle.makeWordRed(payload.getClass().getSimpleName()));
            }else{
                Logger.normal("Current Payload: "+ColorStyle.makeWordRed("null"));
            }
            Printer.printCandidates("payloads", clazz, true, console.payloads);
        }

        if("bullets".equals(type) && payload != null){
            Class<?> clazz = payload.getClass();
            if(bullet != null){
                Logger.normal("Current Bullet: "+ColorStyle.makeWordRed(bullet.getClass().getSimpleName()));
            }else{
                Logger.normal("Current Bullet: "+ColorStyle.makeWordRed("null"));
            }
            Printer.printCandidates("bullets", clazz, true, console.bullets);
        }
    }

    public void printSessionDetails(){
        Logger.success("print current session settings!");

        if(isExploit && exploit != null){
            Class<?> clazz = exploit.getClass();
            Logger.normal("Current Exploit: "+ColorStyle.makeWordRed(clazz.getSimpleName()));
            if(payload == null){
                Printer.printCandidates("payloads", clazz, false, null);
            }
            Printer.printSettings(clazz, settings.get("exploit"));
        }

        if(payload != null){
            Class<?> clazz = payload.getClass();
            Logger.normal("Current Payload: "+ColorStyle.makeWordRed(clazz.getSimpleName()));
            Serializer serializer = payload.getSerializer();
            Logger.normal("Current Serializer Encoder: "+ColorStyle.makeWordRed(serializer.getEncoder()));
            Logger.normal("Current Serializer Output Type: "+ColorStyle.makeWordRed(serializer.getOutputType()));
            if(bullet == null){
                Printer.printCandidates("bullets", clazz, false, null);
            }
        }

        if(bullet != null){
            Class<?> clazz = bullet.getClass();
            Logger.normal("Current Bullet: "+ColorStyle.makeWordRed(clazz.getSimpleName()));
            Printer.printSettings(clazz, settings.get("bullet"));
        }
    }

    public void run() throws Exception {
        Object obj = null;
        if(payload != null && bullet != null){
            payload.setBullet(bullet);
            obj = payload.getObject();
        }

        if(isExploit){
            if(exploit.isRunning()){
                Logger.success("Pre exploit is running now, plz wait to exploit!");
                return;
            }
            if(exploit.has("payload")){
                exploit.set("payload", obj);
            }
            exploit.setStatus(ysomap.common.util.Status.RUNNING);
            new Thread(exploit).start();
        }else if(obj != null){
            SerializerFactory.serialize(
                    payload.getClass().getSimpleName(),
                    payload.getSerializer(),
                    obj);
        }else{
            Logger.error("Something is wrong, plz check options again!");
        }
    }

    public void close(){
        if(isRunning() && exploit != null){
            exploit.stop();
        }
    }

    public void clear(){
        status.clear();
        close();
        exploit = null;
        payload = null;
        bullet = null;
        isExploit = false;
        isEmpty = true;
        settings.get("exploit").clear();
        settings.get("payload").clear();
        settings.get("bullet").clear();
    }

    @Override
    public String toString() {
        String exploitDetail = exploit==null?"null":exploit.toString();
        String payloadClazz = payload==null?"null":payload.getClass().getSimpleName();
        String bulletClazz = bullet==null?"null":bullet.getClass().getSimpleName();
        return "Session{" +
                "\nexploit=" + exploitDetail +
                "\n, payload=" + payloadClazz +
                "\n, bullet=" + bulletClazz +
                "\n}";
    }

    public boolean isEmpty(){
        return isEmpty;
    }

    public boolean isRunning(){
        if(exploit != null){
            return exploit.isRunning();
        }
        return false;
    }
}
