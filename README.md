# FinGuard — Real-Time Fraud Detection System

FinGuard is a high-throughput, real-time fraud monitoring engine built in pure Java. It processes transaction streams, applies rolling time-window analysis, computes dynamic risk scores, and dispatches structured fraud alerts to help prevent financial crimes.

## 🚀 Key Features

- **Real-Time Monitoring**: Processes high-volume transaction streams with low latency.
- **Rolling Window Analysis**: Detects fraud patterns within configurable time windows (default: 10 minutes).
- **Multi-Factor Detection**:
  - **High-Value Transactions**: Flags transactions exceeding a set threshold.
  - **Spending Spikes**: Monitors total debit volume per account within the window.
  - **Location Anomalies**: Detects rapid movement between different cities.
  - **Device Swaps**: Monitors frequent hardware changes for a single account.
- **Tiered Alerting**: Dispatches alerts based on risk severity (Low, Medium, High, Critical).
- **Resource Efficient**: Operates in-memory without external framework dependencies.

## 🛠️ Project Structure

- `src/`: Java source files containing the core engine logic.
- `resources/`: Configuration files and sample transaction data.
- `out/`: Compiled bytecode (generated after build).
- `fraud_alert.txt`: Output file where detected fraud alerts are recorded.

## ⚙️ Prerequisites

- **Java Development Kit (JDK) 8** or higher.
- A terminal or command prompt (Windows/macOS/Linux).

## 🏃 How to Run

### Windows

You can use the provided `run.bat` script to automate compilation and execution.

1.  **Default Simulation** (2000 transactions):
    ```powershell
    .\run.bat
    ```
2.  **Custom Simulation Count**:
    ```powershell
    .\run.bat --simulate 5000
    ```
3.  **Process Transaction File**:
    ```powershell
    .\run.bat --file
    ```

### Manual Compilation & Execution (Generic)

1.  **Compile**:
    ```bash
    mkdir out
    javac -d out -sourcepath src src/*.java
    ```
2.  **Run**:
    ```bash
    # Run simulation
    java -cp out FinGuardMain --simulate 2000
    
    # Run from file
    java -cp out FinGuardMain --file resources/transactions.csv
    ```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
*Developed for IBM Hackathon 2026 by Shadow Coders.*
