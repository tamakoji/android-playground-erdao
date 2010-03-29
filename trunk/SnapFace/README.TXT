 * Copyright (C) 2010 Huan Erdao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.

- REQUIREMENTS:
  + Android SDK 1.5 or higher
  + Android NDK r1 or higer

- SOURCE CHECKOUT and Project Import

  After checkout the root folder, you need to import project
  to eclipse from SnapFace/project folder. root folder contain Application.mk
  which is root for NDK and it is not root for Android Project.

- BEFORE BUILD NDK LIB:
  + Make sure you have properly installed Android NDK and set $NDKROOT.
  + make symbolic link at $NDKROOT/apps with name LaughingMan, directing
    this source root.
    it will be like :
    $NDKROOT/apps/SnapFace --> /home/hoge/workspace/SnapFace

  + after that, just compile library by typing:
    make APP=SnapFace

  it will automatically builds .so file and all you need is to
  build android application from workspace.


