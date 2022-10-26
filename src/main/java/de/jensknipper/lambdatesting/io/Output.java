package de.jensknipper.lambdatesting.io;

public class Output {
  private static final Output EMPTY = new Output();

  private String bucket;
  private String key;
  private String fileLink;

  public Output() {}

  public Output(String bucket, String key, String fileLink) {
    this.bucket = bucket;
    this.key = key;
    this.fileLink = fileLink;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getFileLink() {
    return fileLink;
  }

  public void setFileLink(String fileLink) {
    this.fileLink = fileLink;
  }
}
