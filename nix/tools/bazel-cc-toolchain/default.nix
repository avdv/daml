{ stdenv
, binutils
, bintools
, buildEnv
, darwin
, llvmPackages_12
, makeWrapper
, wrapCCWith
, overrideCC
, runCommand
, writeTextFile
, sigtool
}:


# XXX On Darwin, workaround
# https://github.com/NixOS/nixpkgs/issues/42059. See also
# https://github.com/NixOS/nixpkgs/pull/41589.
let
  postLinkSignHook =
    writeTextFile {
      name = "post-link-sign-hook";
      executable = true;

      text = ''
        CODESIGN_ALLOCATE=${darwin.cctools}/bin/codesign_allocate \
          ${sigtool}/bin/codesign -f -s - "$linkerOutput"
      '';
    };
  darwinBinutils = darwin.binutils.override { inherit postLinkSignHook; };
  cc-darwin =
    with darwin.apple_sdk.frameworks;
    let
      stdenv = llvmPackages_12.stdenv;
      mycc =
        stdenv.cc.override {
          bintools = bintools.override { inherit postLinkSignHook; };
        };
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
                     -isystem ${llvmPackages_12.libcxx}/include/c++/v1 \
                     -F${CoreFoundation}/Library/Frameworks \
                     -F${CoreServices}/Library/Frameworks \
                     -F${Security}/Library/Frameworks \
                     -F${Foundation}/Library/Frameworks \
                     -L${llvmPackages_12.libcxx}/lib \
                     -L${darwin.libobjc}/lib"
    '';

  cc-linux =
    wrapCCWith {
      cc = stdenv.cc.overrideAttrs (oldAttrs: {
        hardeningUnsupportedFlags =
          ["fortify"] ++ oldAttrs.hardeningUnsupportedFlags or [];
      });
    };

  customStdenv =
    if stdenv.isDarwin
    then overrideCC stdenv cc-darwin
    else overrideCC stdenv cc-linux;
in
buildEnv {
  name = "bazel-cc-toolchain";
  paths = [ customStdenv.cc ] ++ (if stdenv.isDarwin then [ darwinBinutils ] else [ binutils ]);
  ignoreCollisions = true;
}
