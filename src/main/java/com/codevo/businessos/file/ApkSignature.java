package com.codevo.businessos.file;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import com.codevo.businessos.common.DomainException;

/** Bounded structural validation of private Android deliverables; archives are never extracted or executed. */
public final class ApkSignature {
    private ApkSignature() {}
    public static void validate(byte[] bytes) {
        boolean manifest=false,dex=false,resources=false;long expanded=0;int entries=0;Set<String> names=new HashSet<>();
        try(var zip=new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for(ZipEntry entry;(entry=zip.getNextEntry())!=null;) {
                String name=entry.getName().replace('\\','/');
                if(++entries>10000 || !names.add(name) || name.startsWith("/") || name.indexOf('\0')>=0 || Arrays.asList(name.split("/")).contains("..")) throw DomainException.invalid("Invalid APK archive entries");
                if(entry.isDirectory()) continue;
                byte[] header=new byte[8];int headerSize=0;byte[] buffer=new byte[8192];
                for(int read;(read=zip.read(buffer))!=-1;) {
                    expanded+=read;if(expanded>64L*1024*1024) throw DomainException.invalid("APK expanded content exceeds 64 MB");
                    int copy=Math.min(read,8-headerSize);if(copy>0){System.arraycopy(buffer,0,header,headerSize,copy);headerSize+=copy;}
                }
                if(name.equals("AndroidManifest.xml")) manifest=headerSize>=4 && (header[0]==3 && header[1]==0 && header[2]==8 && header[3]==0 || header[0]=='<');
                if(name.matches("classes[0-9]*\\.dex")) dex=headerSize>=8 && header[0]=='d' && header[1]=='e' && header[2]=='x' && header[3]=='\n' && header[7]==0;
                if(name.equals("resources.arsc")) resources=headerSize>=4 && header[0]==2 && header[1]==0;
            }
        } catch(IOException e) { throw DomainException.invalid("Malformed APK archive"); }
        if(!manifest || !dex && !resources) throw DomainException.invalid("Archive is not an Android package");
    }
}
