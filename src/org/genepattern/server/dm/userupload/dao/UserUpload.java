package org.genepattern.server.dm.userupload.dao;

import java.io.File;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.util.SemanticUtil;

@Entity
@Table(name="user_upload")
public class UserUpload {
    static public UserUpload initFromGpFileObj(Context userContext, GpFilePath fileObj) {
        UserUpload uf = new UserUpload();
        uf.setUserId(userContext.getUserId());
        uf.setPath(fileObj.getRelativePath());
        uf.init(fileObj.getServerFile());
        return uf;
    }
    
    @Column(name = "user_id")
    private String userId;
    
    @Id
    @GeneratedValue
    private long id;
    
    private String path;
    private String name;
    
    @Column(name = "last_modified")
    private Date lastModified;

    @Column(name = "file_length")
    private long fileLength;
    private String extension;
    private String kind;
    
    @Column(name = "num_parts")
    private int numParts = 1;
    
    @Column(name = "num_parts_recd")
    private int numPartsRecd = 0;
    
    public void init(File file) {
        name = file.getName();
        int idx = name.lastIndexOf('.');
        if (idx > 0 && idx < name.length() - 1) {
            setExtension(name.substring(idx+1));
        }
        
        if (file.exists()) {
            setLastModified(new Date(file.lastModified()));
            setFileLength(file.length());
            setExtension(SemanticUtil.getExtension(file));
            setKind(SemanticUtil.getKind(file));
            if (file.isDirectory()) {
                setKind("directory");
            }
        }
    }
    
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public long getFileLength() {
        return fileLength;
    }

    public void setFileLength(long fileLength) {
        this.fileLength = fileLength;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }
    public int getNumParts() {
        return numParts;
    }
    public void setNumParts(int numParts) {
        this.numParts = numParts;
    }
    public int getNumPartsRecd() {
        return numPartsRecd;
    }
    public void setNumPartsRecd(int numPartsRecd) {
        this.numPartsRecd = numPartsRecd;
    }

}
