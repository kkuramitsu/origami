/***********************************************************************
 * Copyright 2017 Kimio Kuramitsu and ORIGAMI project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***********************************************************************/

package blue.nez.ast;

public interface Source {

	public String getResourceName();

	public long length();

	public int byteAt(long pos);

	public boolean eof(long pos);

	public boolean match(long pos, byte[] text);

	public String subString(long startIndex, long endIndex);

	public byte[] subBytes(long startIndex, long endIndex);

	public Source subSource(long startIndex, long endIndex);

	public long linenum(long pos);

	public int column(long pos);

	// //
	//
	// public String getLineString(long pos);
	//
	// public String formatPositionLine(String messageType, long pos, String
	// message);

}
