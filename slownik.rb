%w[                    pp rubygems                    ].each { |x| require x }
gem 'libxml-ruby', '>= 0.8.3'
require 'xml'



# skip-lines - które linie przy przetwarzaniu należy opuścić
# merged_liens - jeśli linie w plikach ciągną się przez dwie kolumny, ustawić na tak
# double_lines - które linie zawierają hasło dwuwyrazowe
# cord wspołrzędne (z xml), w między którymi znajdują się hasła(1- lewa kolumna, 2-prawa)

@data = {
        "p0123" => {:skip_lines => [1], :double_lines => [2, 6, 22], :cord_from_1 => 100, :cord_to_1 => 110, :cord_from_2 => 1120, :cord_to_2 => 1160},
        "p0124" => {:skip_lines => [1], :double_lines => [2, 18, 20, 34, 88], :cord_from_1 => 190, :cord_to_1 => 211, :cord_from_2 => 1240, :cord_to_2 => 1263},
        "p0125" => {:skip_lines => [1], :double_lines => [21, 62, 120, 138], :cord_from_1 => 110, :cord_to_1 => 121, :cord_from_2 => 1120, :cord_to_2 => 1160},
        "p0126" => {:skip_lines => [1], :double_lines => [71, 80], :cord_from_1 => 200, :cord_to_1 => 222, :cord_from_2 => 1200, :cord_to_2 => 1270},
        "p0127" => {:skip_lines => [1], :double_lines => [57, 68, 107, 110, 124, 151], :cord_from_1 => 100, :cord_to_1 => 113, :cord_from_2 => 1120, :cord_to_2 => 1160},
        "p0128" => {:skip_lines => [1], :double_lines => [24, 63, 81, 89, 166], :cord_from_1 => 100, :cord_to_1 => 222, :cord_from_2 => 1200, :cord_to_2 => 1270},
        "p0129" => {:merged_lines => true, :skip_lines => [1], :double_lines => [21, 24, 37, 43, 132,], :cord_from_1 => 100, :cord_to_1 => 121, :cord_from_2 => 1120, :cord_to_2 => 1174},
        "p0130" => {:skip_lines => [1], :double_lines => [15, 59, 174], :cord_from_1 => 100, :cord_to_1 => 222, :cord_from_2 => 1203, :cord_to_2 => 1270},
        "p0131" => {:skip_lines => [1, 88, 89, 90], :double_lines => [44, 49, 85], :last_line => 70, :cord_from_1 => 100, :cord_to_1 => 110, :cord_from_2 => 1120, :cord_to_2 => 1160}
}


def parse_xml(file_path)
  xml = File.read(file_path)
  file_name = get_filename(file_path)
  parser, parser.string = XML::Parser.new, xml
  doc = parser.parse
  @words = build_words_table(doc, file_name)
  lines = doc.find('//LINE')
  @output += words_with_definition(lines, file_name, :left)
  @output += words_with_definition(lines, file_name, :right)
end


def build_words_table(doc, file_name)
  line_i = 0
  words = []
  doc.find('//LINE').each do |line|
    line_i += 1
    next if @data[file_name][:skip_lines].include?(line_i)
    first_word = true
    line.find('WORD').each_with_index do |word, i|
      if first_word && word.name == 'WORD'
        first_word = false unless @data[file_name][:merged_lines]
        attr = word.attributes.get_attribute('coords')
        if attr
          cord = attr.value.split(',').first
          next if (word.content =~ /^(\w|[ĄĆĘŁŃÓŚŹŻ])/).nil?
          if where = is_new?(cord.to_i, file_name)
            if @data[file_name][:double_lines].include?(line_i)
              words << {:word => "#{word.content} #{line.find('WORD')[i+1].content}", :line => line_i, :where => where}
            else
              words << {:word => word.content, :line => line_i, :where => where}
            end
          end

        end
      end
    end
  end
  words
end

def words_with_definition(lines, file_name, where)
  output = []
  words = @words.select { |w| w[:where] == where }
  words.each_with_index do |w, i|
    next_word = words[i+1]
    out = []
    start = w[:line]-1
    ends = next_word ? next_word[:line]-2 : (@data[file_name][:last_line] ? @data[file_name][:last_line] : lines.length-1)
    ends = [start, ends].max
    (start..ends).each do |i|
      line = lines[i]
      next unless line
      line.children.each do |wo|
        attr = wo.attributes.get_attribute('coords')
        next unless attr
        cord = attr.value.split(',').first.to_i
        if where == :left
          out << wo.content.gsub(/\s+/, "") if cord < @data[file_name][:cord_from_2]
        else
          out << wo.content.gsub(/\s+/, "") unless cord < @data[file_name][:cord_from_2]
        end
      end
    end
    output << w[:word]
    tmp = out.join(" ")
    tmp.slice!("#{w[:word]} ")
    output << tmp.gsub(/\w(-\s)/, "")
  end
  output
end


def is_new?(cord, filepath)
  return :left if (cord >= @data[filepath][:cord_from_1]) && (cord <= @data[filepath][:cord_to_1])
  return :right if ((cord >= @data[filepath][:cord_from_2]) && (cord <= @data[filepath][:cord_to_2]))
  return false
end


def xml_file(file)
  "#{file}.xml"
end

def get_filename(file)
  file.gsub(/(\..*)/, "")
end

def main
  @output = []
  files = ARGV
  files.sort
  files.each do |f|
    system("djvutoxml #{f} #{xml_file(f)}")
  end
  files.collect { |f| xml_file(f) }.each do |f|
    parse_xml(f)
  end
  out = @output.join("\n")
  File.open("output.txt", 'w') { |f| f.write(out) }
end

main