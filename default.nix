{
  sources ? import ./nix/sources.nix,
  system ? builtins.currentSystem,
  pkgs ?
    import sources.nixpkgs {
      overlays = [];
      config = {};
      inherit system;
    },
  doCheck ? true,
  buildGitHubPages ? true,
}:
pkgs.stdenvNoCC.mkDerivation rec {
  pname = "DiffDetective";
  # The single source of truth for the version number is stored in `pom.xml`.
  # Hence, this XML file needs to be parsed to extract the current version.
  version = pkgs.lib.removeSuffix "\n" (pkgs.lib.readFile
    (pkgs.runCommandLocal "DiffDetective-version" {}
      "${pkgs.xq-xml}/bin/xq -x '/project/version' ${./pom.xml} > $out"));
  src = with pkgs.lib.fileset;
    toSource {
      root = ./.;
      fileset = gitTracked ./.;
    };

  nativeBuildInputs = with pkgs; [
    maven
    makeWrapper
    graphviz
  ] ++ pkgs.lib.optional buildGitHubPages (ruby.withPackages (pkgs: with pkgs; [
    github-pages
    jekyll-theme-cayman
  ]));

  mavenRepo = pkgs.stdenv.mkDerivation {
    pname = "${pname}-mavenRepo";
    inherit version;
    src = pkgs.lib.sourceByRegex ./. ["^pom.xml$" "^local-maven-repo(/.*)?$"];

    nativeBuildInputs = with pkgs; [maven];

    buildPhase = ''
      runHook preBuild

      mvn org.apache.maven.plugins:maven-dependency-plugin:3.6.0:go-offline -Dmaven.repo.local="$out"

      runHook postBuild
    '';

    # keep only *.{pom,jar,sha1,nbm} and delete all ephemeral files with lastModified timestamps inside
    installPhase = ''
      runHook preInstall

      find "$out" -type f \
        \( -not \( -name "*.pom" -o -name "*.jar" -o -name "*.sha1" -o -name "*.nbm" \) \
            -o -name "maven-metadata*" \) \
        -delete

      runHook postInstall
    '';

    dontFixup = true;
    dontConfigure = true;
    outputHashAlgo = "sha256";
    outputHashMode = "recursive";
    outputHash = "sha256-LJQfV426han/+H9ejUla7JvN1LS/c9l3e7hODs4Z7Kg=";
  };

  outputs = ["out" "maven"];

  jre-minimal = pkgs.callPackage (import "${sources.nixpkgs}/pkgs/development/compilers/openjdk/jre.nix") {
    modules = ["java.base" "java.desktop"];
  };

  buildPhase = ''
    runHook preBuild

    mvn() {
      command mvn --offline -Dmaven.repo.local="$mavenRepo" "$@"
    }

    ${
      # Build the documentation before the code because we don't want to include
      # the generated files in the GitHub Pages
      if buildGitHubPages
      then ''
        mvn javadoc:javadoc
        JEKYLL_ENV=production PAGES_REPO_NWO=VariantSync/DiffDetective JEKYLL_BUILD_REVISION= github-pages build
        rm -rf _site/target
      ''
      else ""
    }
    mvn -Dmaven.test.skip=true clean package

    runHook postBuild
  '';

  inherit doCheck;
  checkPhase = ''
    runHook preTest

    mvn --offline -Dmaven.repo.local="$mavenRepo" test

    runHook postTest
  '';

  installPhase = ''
    runHook preInstall

    install -Dm644 "target/diffdetective-${version}.jar" "$out/share/java/DiffDetective.jar"
    local jar="$out/share/java/DiffDetective/DiffDetective.jar"
    install -Dm644 "target/diffdetective-${version}-jar-with-dependencies.jar" "$jar"
    makeWrapper "${jre-minimal}/bin/java" "$out/bin/DiffDetective" --add-flags "-cp \"$jar\"" \
      --prefix PATH : "${pkgs.graphviz}/bin"

    ${
      if buildGitHubPages
      then ''
        mkdir "$out/share/github-pages"
        cp -r _site "$out/share/github-pages/DiffDetective"
      ''
      else ""
    }

    cp -r "$mavenRepo" "$maven"
    chmod u+w -R "$maven"
    mvn --offline -Dmaven.repo.local="$maven" -Dmaven.test.skip=true install

    # keep only *.{pom,jar,sha1,nbm} and delete all ephemeral files with lastModified timestamps inside
    find "$maven" -type f \
      \( -not \( -name "*.pom" -o -name "*.jar" -o -name "*.sha1" -o -name "*.nbm" \) \
          -o -name "maven-metadata*" \) \
      -delete

    runHook postInstall
  '';

  meta = {
    description = "DiffDetective is a library for analysing changes to software product lines";
    homepage = "https://github.com/VariantSync/DiffDetective";
    license = pkgs.lib.licenses.lgpl3;
    platforms = pkgs.maven.meta.platforms;
    maintainers = [
      {
        name = "Benjamin Moosherr";
        email = "Benjamin.Moosherr@uni-ulm.de";
        github = "ibbem";
        githubId = 61984399;
      }
    ];
  };
}
