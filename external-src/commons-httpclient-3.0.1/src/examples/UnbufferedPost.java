/*
 * $Header$
 * $Revision$
 * $Date$
 * ====================================================================
 *
 *  Copyright 2002-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * [Additional notices, if required by prior licensing conditions]
 *
 */
import java.io.File;
import java.io.FileInputStream;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;

/**
 * Example how to use unbuffered POST request.
 *
 * @author Oleg Kalnichevski
 */
public class UnbufferedPost {

  public static void main(String[] args) throws Exception {
    if (args.length != 1)  {
        System.out.println("Usage: ChunkEncodedPost <file>");
        System.out.println("<file> - full path to a file to be posted");
        System.exit(1);
    }
    HttpClient client = new HttpClient();

    PostMethod httppost = new PostMethod("http://localhost:8080/httpclienttest/body");

    File file = new File(args[0]);
    httppost.setRequestEntity(new InputStreamRequestEntity(
        new FileInputStream(file), file.length()));

    try {
        client.executeMethod(httppost);

        if (httppost.getStatusCode() == HttpStatus.SC_OK) {
            System.out.println(httppost.getResponseBodyAsString());
        } else {
          System.out.println("Unexpected failure: " + httppost.getStatusLine().toString());
        }
    } finally {
        httppost.releaseConnection();
    }
  }
}
