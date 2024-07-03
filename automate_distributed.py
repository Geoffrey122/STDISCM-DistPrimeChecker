import subprocess
import time
import csv
import statistics
import os
import signal
import platform

def update_thread_count(file_name, thread_count):
    """Update the MAX_THREAD_COUNT in the specified Java file."""
    with open(file_name, 'r') as file:
        lines = file.readlines()

    with open(file_name, 'w') as file:
        for line in lines:
            if "private static final int MAX_THREAD_COUNT" in line:
                file.write(f'    private static final int MAX_THREAD_COUNT = {thread_count}; // Number of threads\n')
            else:
                file.write(line)

def run_master_client():
    """Runs the MasterClient program and returns the runtime in milliseconds."""
    result = subprocess.run(['java', 'MasterClient'], capture_output=True, text=True)
    with open('masterclient_log.txt', 'a') as log_file:
        log_file.write(result.stdout)
    runtime = None
    for line in result.stdout.split('\n'):
        if line.startswith('Runtime:'):
            runtime = int(line.split(':')[1].strip().split()[0])
    if runtime is None:
        raise RuntimeError("Failed to capture runtime from MasterClient output.")
    return runtime

def run_experiment(thread_counts, num_runs):
    results = []
    java_files = ['MasterClient.java', 'Slave.java']
    for thread_count in thread_counts:
        print(f'Running experiment with {thread_count} threads...')

        # Update thread count in all relevant Java files
        for java_file in java_files:
            update_thread_count(java_file, thread_count)

        subprocess.run(['javac', 'MasterClient.java', 'Slave.java'])

        # Start the slave process
        slave_process = subprocess.Popen(['java', 'Slave'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)

        # Let the slave start up
        time.sleep(5)

        runtimes = []
        for _ in range(num_runs):
            for _ in range(3):  # Run 3 times to allow OS to cache memory locations
                runtime = run_master_client()
                if runtime:
                    runtimes.append(runtime)
                time.sleep(1)  # Give some time between runs

        # Calculate the mean runtime
        if runtimes:
            mean_runtime = statistics.mean(runtimes)
            results.append((thread_count, mean_runtime))
        else:
            results.append((thread_count, None))

        # Terminate the slave process
        terminate_process(slave_process)

    return results

def terminate_process(process):
    if process.poll() is None:  # Check if the process is still running
        if platform.system() == "Windows":
            subprocess.run(['taskkill', '/F', '/T', '/PID', str(process.pid)])
        else:
            os.kill(process.pid, signal.SIGTERM)
        process.wait()  # Wait for the process to terminate

def save_to_csv(results, filename):
    with open(filename, mode='w', newline='') as file:
        writer = csv.writer(file)
        writer.writerow(['Thread Count', 'Mean Runtime (ms)'])
        writer.writerows(results)

if __name__ == "__main__":
    thread_counts = [2**i for i in range(11)]  # 1, 2, 4, ..., 1024
    num_runs = 5

    results = run_experiment(thread_counts, num_runs)
    save_to_csv(results, 'results_combined.csv')
    print('Experiment completed and results saved to results_combined.csv')
