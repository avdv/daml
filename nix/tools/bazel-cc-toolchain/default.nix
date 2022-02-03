{ stdenv
, binutils
, bintools
, buildEnv
, darwin
, llvmPackages_7
, makeWrapper
, overrideCC
, runCommand
, writeTextFile
, sigtool
}:


# XXX On Darwin, workaround
# https://github.com/NixOS/nixpkgs/issues/42059. See also
# https://github.com/NixOS/nixpkgs/pull/41589.
let
  mycc =
    stdenv.cc.override {
      bintools = bintools.override {
        postLinkSignHook = writeTextFile {
          name = "post-link-sign-hook";
          executable = true;

          text = ''
            CODESIGN_ALLOCATE=${darwin.cctools}/bin/codesign_allocate \
              ${sigtool}/bin/codesign -f -s - "$linkerOutput"
          '';
        };
      };
    };
  cc-darwin =
    with darwin.apple_sdk.frameworks;
    # Note (MK): For now we pin to clang 7 since newer versions fail to build abseil.
    let
      stdenv = llvmPackages_7.stdenv;
    in
    runCommand "cc-wrapper-bazel"
    {
      buildInputs = [ mycc makeWrapper ];
    }
    ''
      mkdir -p $out/bin

      # Copy the content of pkgs.stdenv.cc
      for i in ${mycc}/bin/*
      do
        ln -sf $i $out/bin
      done

      # Override cc
      rm $out/bin/cc $out/bin/clang $out/bin/clang++

      makeWrapper ${mycc}/bin/cc $out/bin/cc \
        --set CODESIGN_ALLOCATE ${darwin.cctools}/bin/codesign_allocate \
        --prefix PATH : $out/bin \
        --add-flags "-Wno-unused-command-line-argument \
                     -mmacosx-version-min=10.14 \
                     -F${CoreFoundation}/Library/Frameworks \
                     -F${CoreServices}/Library/Frameworks \
                     -F${Security}/Library/Frameworks \
                     -F${Foundation}/Library/Frameworks \
                     -L${llvmPackages_7.libcxx}/lib \
                     -L${darwin.libobjc}/lib"
    '';

  cc-linux =
    runCommand "cc-wrapper-bazel" {
      buildInputs = [ makeWrapper ];
    }
    ''
      mkdir -p $out/bin

      # Copy the content of pkgs.stdenv.cc
      for i in ${stdenv.cc}/bin/*
      do
        ln -sf $i $out/bin
      done

      # Override gcc
      rm $out/bin/cc $out/bin/gcc $out/bin/g++

      # We disable the fortify hardening as it causes issues with some
      # packages built with bazel that set these flags themselves.
      makeWrapper ${stdenv.cc}/bin/cc $out/bin/cc \
        --set hardeningDisable fortify
    '';

  customStdenv =
    if stdenv.isDarwin
    then overrideCC stdenv cc-darwin
    else overrideCC stdenv cc-linux;
in
buildEnv {
  name = "bazel-cc-toolchain";
  paths = [ customStdenv.cc ] ++ (if stdenv.isDarwin then [ darwin.binutils ] else [ binutils ]);
}
