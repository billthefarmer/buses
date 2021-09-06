BEGIN {
    FS = "\",\"";
}

{
    if ($2 ~ /[a-z]{8}/)
        a[substr($2, 0, 3)] = NR;
}

END {
    for (s in a)
        print s;
}
