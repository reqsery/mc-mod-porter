package com.autoporter;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.regex.*;
import java.net.URI;

/** Swing front end for the existing Auto-Porter CLI workflow. */
public class MainWindow extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final String AUTO_DETECT = "Auto Detect";
    static final String REMAP_AUTO = "Auto (Yarn → 26.x)";
    static final String REMAP_ALWAYS = "Always";
    static final String REMAP_NEVER = "Never";

    private JButton createLinkButton(String text, String url, String iconName) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        java.net.URL iconUrl = MainWindow.class.getResource("/icons/" + iconName + ".png");
        if (iconUrl != null) {
            ImageIcon icon = new ImageIcon(iconUrl);
            button.setIcon(new ImageIcon(icon.getImage().getScaledInstance(16, -1, Image.SCALE_SMOOTH)));
            button.setIconTextGap(5);
        }
        button.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> openUrl(url));
        return button;
    }

    private void openUrl(String url) {
        try {
            if (!Desktop.isDesktopSupported()
                    || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                throw new IOException("Opening links is not supported.");
            }

            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            showError("Could not open link:\n" + url);
        }
    }
    private final JTextField projectPathField = new JTextField();
    private final JComboBox<String> sourceVersionCombo = new JComboBox<>();
    private final JComboBox<String> targetVersionCombo = new JComboBox<>();
    private final JComboBox<String> loaderCombo = new JComboBox<>(new String[] { AUTO_DETECT, "Fabric", "NeoForge" });
    private final JComboBox<String> mappingsCombo = new JComboBox<>(new String[] { AUTO_DETECT, "Yarn", "Mojang" });
    private final JComboBox<String> remapCombo = new JComboBox<>(new String[] { REMAP_AUTO, REMAP_ALWAYS, REMAP_NEVER });
    private final JButton browseButton = new JButton("Browse...");
    private final JButton portButton = new JButton("Port Mod");
    private final JButton buildButton = new JButton("Build");
    private final JButton runClientButton = new JButton("Run Client");
    private final JButton openFolderButton = new JButton("Open Folder");
    private final JButton cancelButton = new JButton("Cancel");
    private final JLabel statusValueLabel = new JLabel("Ready");
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JTextArea logArea = new JTextArea("Ready.\n");

    private volatile Process activeProcess;
    private SwingWorker<Integer, String> activeWorker;
    private Path portedProject;

    public static void main(String[] args) { launch(); }

    public static void launch() {
        FlatLightLaf.setup();
        UIManager.put("Button.arc", 0);
        UIManager.put("Component.arc", 0);
        UIManager.put("TextComponent.arc", 0);
        UIManager.put("ProgressBar.arc", 0);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Button.focusedBorderColor", new Color(120, 120, 120));
        EventQueue.invokeLater(() -> new MainWindow().setVisible(true));
    }

    public MainWindow() {
        super("MC Mod Porter");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setBounds(100, 100, 900, 745);
        setResizable(false);
        java.net.URL appIcon = MainWindow.class.getResource("/icons/app.png");
        if (appIcon != null) setIconImage(new ImageIcon(appIcon).getImage());
        buildUi();
        wireActions();
        setWorking(false);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) {
                cancelActiveWork();
                dispose();
            }
        });
    }

    private void buildUi() {
        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(12, 12, 12, 12));
        contentPane.setLayout(null);
        setContentPane(contentPane);

        JPanel projectPanel = new JPanel();
        projectPanel.setLayout(null);
        projectPanel.setBorder(new javax.swing.border.LineBorder(new Color(185, 185, 185), 1));
        projectPanel.setBounds(20, 20, 845, 225);
        contentPane.add(projectPanel);

        JLabel projectLabel = new JLabel("Project");
        projectLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        projectLabel.setBounds(18, 15, 100, 20);
        projectPanel.add(projectLabel);

        projectPathField.setBounds(18, 40, 680, 30);
        projectPanel.add(projectPathField);

        browseButton.setBounds(708, 40, 115, 30);
        projectPanel.add(browseButton);

        JLabel sourceLabel = new JLabel("Source Version");
        sourceLabel.setBounds(18, 88, 120, 20);
        projectPanel.add(sourceLabel);

        sourceVersionCombo.addItem(AUTO_DETECT);
        for (String version : VersionDatabase.allVersions()) {
            sourceVersionCombo.addItem(version);
            targetVersionCombo.addItem(version);
        }

        sourceVersionCombo.setBounds(18, 110, 230, 30);
        projectPanel.add(sourceVersionCombo);

        JLabel targetLabel = new JLabel("Target Version");
        targetLabel.setBounds(300, 88, 120, 20);
        projectPanel.add(targetLabel);

        targetVersionCombo.setBounds(300, 110, 230, 30);
        targetVersionCombo.setSelectedItem("1.21.11");
        projectPanel.add(targetVersionCombo);

        JLabel loaderLabel = new JLabel("Loader");
        loaderLabel.setBounds(580, 88, 100, 20);
        projectPanel.add(loaderLabel);

        loaderCombo.setBounds(580, 110, 243, 30);
        projectPanel.add(loaderCombo);

        JLabel mappingsLabel = new JLabel("Source Mappings");
        mappingsLabel.setBounds(18, 152, 150, 20);
        projectPanel.add(mappingsLabel);

        mappingsCombo.setToolTipText("Names used by the mod's sources: Yarn (Fabric) or Mojang/official. Auto Detect reads the build files.");
        mappingsCombo.setBounds(18, 174, 230, 30);
        projectPanel.add(mappingsCombo);

        JLabel remapLabel = new JLabel("Remap to Mojang Names");
        remapLabel.setBounds(300, 152, 200, 20);
        projectPanel.add(remapLabel);

        remapCombo.setToolTipText("Auto remaps Yarn sources when porting to 26.x (which has no Yarn). Always forces it, Never skips it.");
        remapCombo.setBounds(300, 174, 230, 30);
        projectPanel.add(remapCombo);

        portButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        portButton.setBounds(20, 265, 845, 38);
        contentPane.add(portButton);

        buildButton.setBounds(20, 320, 110, 32);
        contentPane.add(buildButton);

        runClientButton.setBounds(140, 320, 110, 32);
        contentPane.add(runClientButton);

        openFolderButton.setBounds(260, 320, 110, 32);
        contentPane.add(openFolderButton);

        cancelButton.setBounds(755, 320, 110, 32);
        contentPane.add(cancelButton);

        JLabel statusLabel = new JLabel("Status:");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusLabel.setBounds(20, 375, 55, 20);
        contentPane.add(statusLabel);

        statusValueLabel.setBounds(75, 375, 790, 20);
        contentPane.add(statusValueLabel);

        progressBar.setValue(0);
        progressBar.setStringPainted(true); // shows the live percentage (no fixed text)
        progressBar.setBounds(20, 400, 845, 20);
        contentPane.add(progressBar);

        JLabel logsLabel = new JLabel("Logs");
        logsLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logsLabel.setBounds(20, 445, 100, 20);
        contentPane.add(logsLabel);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBounds(20, 470, 845, 180);
        contentPane.add(scrollPane);

        logArea.setEditable(false);
        logArea.setLineWrap(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setText("Ready.");
        scrollPane.setViewportView(logArea);
        JPanel linksPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        linksPanel.setOpaque(false);
        linksPanel.setBounds(15, 660, 850, 28);

        linksPanel.add(createLinkButton("Discord",
            "https://discord.gg/vV2USr9phF", "discord"));
        linksPanel.add(new JLabel("·"));

        linksPanel.add(createLinkButton("GitHub",
            "https://github.com/reqsery/mc-mod-porter", "github"));
        linksPanel.add(new JLabel("·"));

        linksPanel.add(createLinkButton("Contribute",
            "https://github.com/reqsery/mc-mod-porter/blob/main/.github/CONTRIBUTING.md", "contribute"));
        linksPanel.add(new JLabel("·"));

        linksPanel.add(createLinkButton("Report Bug",
            "https://github.com/reqsery/mc-mod-porter/issues/new?template=bug_report.md", "bug"));
        linksPanel.add(new JLabel("·"));

        linksPanel.add(createLinkButton("Suggest Feature",
            "https://github.com/reqsery/mc-mod-porter/issues/new?template=feature_request.md", "feature"));

        contentPane.add(linksPanel);
    }

    private void wireActions() {
        browseButton.addActionListener(event -> browseForProject());
        projectPathField.addActionListener(event -> detectProject(safePath(projectPathField.getText())));
        portButton.addActionListener(event -> startPort());
        buildButton.addActionListener(event -> startGradleTask("build", "Building project"));
        runClientButton.addActionListener(event -> startGradleTask("runClient", "Running client"));
        openFolderButton.addActionListener(event -> openPortedFolder());
        cancelButton.addActionListener(event -> cancelActiveWork());
    }

    private void browseForProject() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select the extracted mod project folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        Path current = safePath(projectPathField.getText());
        if (current != null && Files.isDirectory(current)) chooser.setCurrentDirectory(current.toFile());
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path selected = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
            projectPathField.setText(selected.toString());
            detectProject(selected);
        }
    }

    private void detectProject(Path project) {
        if (project == null || !Files.isDirectory(project)) {
            setStatus("Select a valid project folder");
            return;
        }
        ProjectInfo info = inspectProject(project);
        sourceVersionCombo.setSelectedItem(info.version() == null ? AUTO_DETECT : info.version());
        loaderCombo.setSelectedItem(info.loader() == null ? AUTO_DETECT : info.loader());
        String mappings = "unknown mappings";
        try {
            SourceRemapper.Mappings detected = SourceRemapper.detect(project, info.version() == null ? "" : info.version()).mappings();
            mappingsCombo.setSelectedItem(switch (detected) {
                case YARN -> "Yarn";
                case MOJANG -> "Mojang";
                default -> AUTO_DETECT;
            });
            if (detected == SourceRemapper.Mappings.YARN || detected == SourceRemapper.Mappings.MOJANG) {
                mappings = detected == SourceRemapper.Mappings.YARN ? "Yarn" : "Mojang";
            }
        } catch (IOException ignored) {
            mappingsCombo.setSelectedItem(AUTO_DETECT);
        }
        setStatus("Detected " + (info.version() == null ? "unknown version" : info.version())
            + " / " + (info.loader() == null ? "unknown loader" : info.loader()) + " / " + mappings);
    }

    private void startPort() {
        Path source = safePath(projectPathField.getText());
        if (source == null || !Files.isDirectory(source)) {
            showError("Select the extracted mod source/project folder, not a JAR or ZIP.");
            return;
        }
        source = source.toAbsolutePath().normalize();
        String from = selected(sourceVersionCombo);
        if (AUTO_DETECT.equals(from)) from = inspectProject(source).version();
        String to = selected(targetVersionCombo);
        if (from == null || !VersionDatabase.supports(from)) {
            showError("The source Minecraft version could not be detected. Select it manually.");
            return;
        }
        if (to == null || !VersionDatabase.supports(to)) {
            showError("Select a supported target Minecraft version.");
            return;
        }
        if (from.equals(to)) {
            showError("Source and target versions must be different.");
            return;
        }

        portedProject = AutoPorterMain.outputDirectoryFor(source, to);
        List<String> command = javaCommand();
        command.add(AutoPorterMain.class.getName());
        command.add(source.toString());
        command.add(from);
        command.add(to);
        command.addAll(portFlags(selected(mappingsCombo), selected(remapCombo)));
        startProcess(command, source.getParent(), "Porting mod", true);
    }

    /** CLI flags for the GUI's mappings and remap choices. */
    static List<String> portFlags(String mappings, String remap) {
        List<String> flags = new ArrayList<>();
        if ("Yarn".equals(mappings)) flags.addAll(List.of("--source-mappings", "yarn"));
        if ("Mojang".equals(mappings)) flags.addAll(List.of("--source-mappings", "mojang"));
        if (REMAP_ALWAYS.equals(remap)) flags.add("--remap");
        if (REMAP_NEVER.equals(remap)) flags.add("--no-remap");
        return flags;
    }

    private void startGradleTask(String task, String activity) {
        if (portedProject == null || !Files.isDirectory(portedProject)) {
            showError("Port a project first.");
            return;
        }
        Path wrapper = portedProject.resolve(isWindows() ? "gradlew.bat" : "gradlew");
        if (!Files.isRegularFile(wrapper)) {
            showError("No Gradle wrapper found in " + portedProject);
            return;
        }
        startProcess(new ArrayList<>(List.of(wrapper.toAbsolutePath().toString(), task, "--no-daemon")),
            portedProject, activity, false);
    }

    private void startProcess(List<String> command, Path workingDirectory, String activity, boolean porterProcess) {
        if (activeWorker != null) return;
        logArea.setText("");
        progressBar.setValue(0);
        progressBar.setIndeterminate(!porterProcess);
        setStatus(activity + "...");
        setWorking(true);

        activeWorker = new SwingWorker<>() {
            @Override protected Integer doInBackground() throws Exception {
                if (!porterProcess) BuildRunner.ensureUnixGradleWrapperExecutable(workingDirectory);
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.directory(workingDirectory.toFile());
                builder.redirectErrorStream(true);
                configureJavaHome(builder, workingDirectory);
                activeProcess = builder.start();
                if (porterProcess) {
                    try (OutputStream input = activeProcess.getOutputStream()) {
                        input.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
                    }
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(activeProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while (!isCancelled() && (line = reader.readLine()) != null) publish(line);
                }
                return isCancelled() ? -1 : activeProcess.waitFor();
            }

            @Override protected void process(List<String> lines) {
                for (String line : lines) {
                    appendLog(line);
                    if (porterProcess) updatePortProgress(line);
                }
            }

            @Override protected void done() {
                try {
                    int exit = get();
                    if (exit == 0) {
                        progressBar.setValue(100);
                        setStatus(porterProcess ? "Port complete" : activity + " complete");
                        if (porterProcess) appendLog("Port complete. Found an issue? Use Report Bug below, or join Discord for help.");
                    } else if (exit == -1) setStatus("Cancelled");
                    else if (porterProcess && portedProject != null && Files.isDirectory(portedProject)) {
                        progressBar.setValue(100);
                        setStatus("Completed with issues");
                        appendLog("Port completed with issues. Review the logs, then use Report Bug below if needed.");
                    } else setStatus(activity + " failed (exit " + exit + ")");
                } catch (CancellationException exception) {
                    setStatus("Cancelled");
                } catch (Exception exception) {
                    appendLog("ERROR: " + exception.getMessage());
                    setStatus(activity + " failed");
                } finally {
                    activeProcess = null;
                    activeWorker = null;
                    progressBar.setIndeterminate(false);
                    setWorking(false);
                }
            }
        };
        activeWorker.execute();
    }

    private void configureJavaHome(ProcessBuilder builder, Path project) {
        BuildRunner runner = new BuildRunner();
        int required = runner.detectRequiredJavaVersion(project);
        String javaHome = runner.detectJavaHome(required);
        if (javaHome != null) builder.environment().put("JAVA_HOME", javaHome);
    }

    private void cancelActiveWork() {
        Process process = activeProcess;
        if (process != null && process.isAlive()) {
            List<ProcessHandle> children = process.descendants().toList();
            children.forEach(ProcessHandle::destroy);
            process.destroy();
            children.stream().filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroyForcibly);
            if (process.isAlive()) process.destroyForcibly();
        }
        if (activeWorker != null) activeWorker.cancel(true);
    }

    private void openPortedFolder() {
        if (portedProject == null || !Files.isDirectory(portedProject)) {
            showError("No ported project folder is available yet.");
            return;
        }
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
                throw new IOException("Opening folders is not supported on this desktop.");
            Desktop.getDesktop().open(portedProject.toFile());
        } catch (IOException exception) {
            showError("Could not open the folder: " + exception.getMessage());
        }
    }

    private void setWorking(boolean working) {
        browseButton.setEnabled(!working);
        projectPathField.setEnabled(!working);
        sourceVersionCombo.setEnabled(!working);
        targetVersionCombo.setEnabled(!working);
        loaderCombo.setEnabled(!working);
        mappingsCombo.setEnabled(!working);
        remapCombo.setEnabled(!working);
        portButton.setEnabled(!working);
        boolean hasOutput = portedProject != null && Files.isDirectory(portedProject);
        buildButton.setEnabled(!working && hasOutput);
        runClientButton.setEnabled(!working && hasOutput && inspectProject(portedProject).loader() != null);
        openFolderButton.setEnabled(!working && hasOutput);
        cancelButton.setEnabled(working);
    }

    private void updatePortProgress(String line) {
        PortProgress.parse(line, progressBar.getValue()).ifPresent(update -> {
            progressBar.setValue(update.percent());
            if (update.status() != null) setStatus(update.status());
        });
    }

    private void appendLog(String line) {
        // Consolas has no ✓ / ⚠ glyphs: show their ASCII form instead of empty boxes
        logArea.append(ConsoleText.toAscii(line, logArea.getFont()::canDisplay) + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void setStatus(String status) { statusValueLabel.setText(status); }

    private void showError(String message) {
        setStatus("Ready");
        JOptionPane.showMessageDialog(this, message, "MC Mod Porter", JOptionPane.ERROR_MESSAGE);
    }

    private static String selected(JComboBox<String> combo) {
        Object value = combo.getSelectedItem();
        return value == null ? null : value.toString();
    }

    private static Path safePath(String value) {
        try { return value == null || value.isBlank() ? null : Path.of(value.trim()); }
        catch (InvalidPathException exception) { return null; }
    }

    private static List<String> javaCommand() {
        Path java = Path.of(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java");
        Path launchDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        String absoluteClasspath = Arrays.stream(System.getProperty("java.class.path").split(Pattern.quote(File.pathSeparator)))
            .map(entry -> {
                Path path = Path.of(entry);
                return (path.isAbsolute() ? path : launchDirectory.resolve(path)).normalize().toString();
            })
            .reduce((left, right) -> left + File.pathSeparator + right)
            .orElse("");
        // The log reader decodes UTF-8, so the child must write UTF-8 (a pipe otherwise uses the Windows code page).
        return new ArrayList<>(List.of(java.toString(), "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8",
            "-Dfile.encoding=UTF-8", "-cp", absoluteClasspath));
    }

    static ProjectInfo inspectProject(Path root) {
        if (root == null || !Files.isDirectory(root)) return new ProjectInfo(null, null);
        List<Path> files = projectFiles(root);
        return new ProjectInfo(detectVersion(files), detectLoader(files));
    }

    private static List<Path> projectFiles(Path root) {
        try (var stream = Files.walk(root, 6)) {
            return stream.filter(Files::isRegularFile).filter(path -> {
                String relative = root.relativize(path).toString().replace('\\', '/');
                return !relative.startsWith(".git/") && !relative.startsWith(".gradle/")
                    && !relative.startsWith("build/") && !relative.contains("/build/");
            }).toList();
        } catch (IOException exception) { return List.of(); }
    }

    private static String detectVersion(List<Path> files) {
        for (Path path : files.stream().filter(p -> p.getFileName().toString().equals("gradle.properties"))
                .sorted(Comparator.comparingInt(Path::getNameCount)).toList()) {
            try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                Properties properties = new Properties();
                properties.load(reader);
                String value = properties.getProperty("minecraft_version");
                if (VersionDatabase.supports(value)) return VersionDatabase.normalize(value);
            } catch (IOException ignored) { }
        }
        List<String> versions = new ArrayList<>(VersionDatabase.allVersions());
        versions.sort(Comparator.comparingInt(String::length).reversed());
        for (Path path : files) {
            String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
            if (!(name.endsWith(".gradle") || name.endsWith(".gradle.kts") || name.equals("fabric.mod.json") || name.endsWith("mods.toml"))) continue;
            try {
                String content = Files.readString(path, StandardCharsets.UTF_8);
                for (String candidate : versions) {
                    if (Pattern.compile("(?<![0-9.])" + Pattern.quote(candidate) + "(?![0-9.])").matcher(content).find()) return candidate;
                }
            } catch (IOException ignored) { }
        }
        return null;
    }

    private static String detectLoader(List<Path> files) {
        if (files.stream().anyMatch(p -> p.getFileName().toString().equals("fabric.mod.json"))) return "Fabric";
        if (files.stream().anyMatch(p -> p.getFileName().toString().equals("neoforge.mods.toml"))) return "NeoForge";
        for (Path path : files) {
            String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
            if (!(name.endsWith(".gradle") || name.endsWith(".gradle.kts") || name.equals("gradle.properties"))) continue;
            try {
                String content = Files.readString(path, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
                if (content.contains("net.neoforged") || content.contains("neoforge_version")
                        || Pattern.compile("(?m)^\\s*neo_version\\s*=").matcher(content).find()) return "NeoForge";
            } catch (IOException ignored) { }
        }
        return null;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    record ProjectInfo(String version, String loader) { }
}
